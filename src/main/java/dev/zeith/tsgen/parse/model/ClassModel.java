package dev.zeith.tsgen.parse.model;

import dev.zeith.tsgen.exceptions.ClassModelParseException;
import dev.zeith.tsgen.parse.*;
import dev.zeith.tsgen.parse.sig.*;
import dev.zeith.tsgen.util.TypeUtil;
import lombok.*;
import org.jetbrains.annotations.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static dev.zeith.tsgen.parse.EnumNullability.lstStream;
import static org.objectweb.asm.ClassReader.*;

@Builder
public record ClassModel(
		@Nullable ClassGeneric generic,
		@NotNull Type name,
		@Nullable Type superName,
		@Singular List<Type> interfaces,
		int acc,
		@Singular List<FieldModel> fields,
		@Singular List<MethodModel> methods,
		@Singular List<ConstructorModel> constructors,
		List<AnnotationNode> visibleAnnotations,
		List<AnnotationNode> invisibleAnnotations,
		@Nullable String comment
)
		implements IGeneralModel
{
	public boolean isInterface()
	{
		return (acc & Opcodes.ACC_INTERFACE) != 0;
	}
	
	public boolean isAbstract()
	{
		return (acc & Opcodes.ACC_ABSTRACT) != 0;
	}
	
	public boolean isPublic()
	{
		return (acc & Opcodes.ACC_PUBLIC) != 0;
	}
	
	public String getSimpleName()
	{
		return TypeUtil.getSimpleName(name);
	}
	
	public boolean isFunctionalInterface()
	{
		return isInterface() && findAnnotation(n ->
				n.desc.equals("Ljava/lang/FunctionalInterface;")
		) != null;
	}
	
	public AnnotationNode findAnnotation(Predicate<? super AnnotationNode> predicate)
	{
		return Stream.concat(
				lstStream(visibleAnnotations),
				lstStream(invisibleAnnotations)
		).filter(predicate).findFirst().orElse(null);
	}
	
	public static ClassModel parse(byte[] bytecode)
			throws ClassModelParseException
	{
		return parse(new ClassReader(bytecode));
	}
	
	public static ClassModel parse(ClassReader reader)
			throws ClassModelParseException
	{
		try
		{
			ClassNode node = new ClassNode();
			reader.accept(node, SKIP_CODE | SKIP_FRAMES);
			
			if((node.access & Opcodes.ACC_PRIVATE) != 0)
				return null;
			
			List<FieldModel> fields = new ArrayList<>();
			List<MethodModel> methods = new ArrayList<>();
			List<ConstructorModel> ctors = new ArrayList<>();
			
			boolean isEnum = Objects.equals("java/lang/Enum", node.superName);
			
			for(var n : node.fields)
			{
				boolean isPublic = (n.access & Opcodes.ACC_PUBLIC) != 0;
				if(!isPublic) continue;
				
				boolean isStatic = (n.access & Opcodes.ACC_STATIC) != 0;
				
				fields.add(new FieldModel(
						isStatic,
						n.name,
						NullAwareType.of(-1, EnumNullability.of(n), Type.getType(n.desc), n.signature),
						null
				));
			}
			
			for(var n : node.methods)
			{
				if((n.access & Opcodes.ACC_PUBLIC) == 0) continue;
				
				var gen = SimpleGeneric.parseMethodDescSignature(n.signature);
				
				Type[] argTypes = Type.getArgumentTypes(n.desc);
				List<ParameterNode> params = n.parameters;
				
				boolean isEnumValueOf = isEnum && n.name.equals("valueOf") && argTypes.length == 1 && (n.access & Opcodes.ACC_PUBLIC) != 0 && (n.access & Opcodes.ACC_STATIC) != 0;
				
				List<SimpleGeneric> parametrizedArgs = gen != null ? gen.parameters() : null;
				EnumNullability[] ns = EnumNullability.of(n);
				NullAwareType[] args = new NullAwareType[ns.length];
				for(int i = 0; i < argTypes.length; i++)
				{
					String paramName = null;
					
					// ASM 9+: parameters list is populated if SKIP_DEBUG was NOT used
					if(params != null && i < params.size())
						paramName = params.get(i).name;
					else // ASM 8 fallback: extract from localVariables by index
						if(n.localVariables != null)
							for(LocalVariableNode lv : n.localVariables)
								if(lv.index == i && lv.name != null)
								{
									paramName = lv.name;
									break;
								}
					
					// Fallbacks
					if(paramName == null || paramName.isBlank())
					{
						paramName = isEnumValueOf ? "name" : null;
					}
					
					args[i] = new NullAwareType(
							i,
							paramName,
							ns[i],
							argTypes[i],
							parametrizedArgs != null && i < parametrizedArgs.size() ? parametrizedArgs.get(i) : null
					);
				}
				
				if(n.name.startsWith("<"))
				{
					if(n.name.equals("<init>")) ctors.add(new ConstructorModel(n.access, args, null));
					continue;
				}
				
				methods.add(new MethodModel(
						n.access,
						n.name,
						gen != null ? gen.typeParameters() : null,
						new NullAwareType(-1, "ret", EnumNullability.ofReturnType(n), Type.getReturnType(n.desc), gen != null ? gen.returnType() : null),
						args,
						null
				));
			}
			
			ClassGeneric generic = ClassGeneric.parseClassSignature(node.signature);
			
			return new ClassModel(
					generic,
					Type.getObjectType(node.name),
					node.superName != null ? Type.getObjectType(node.superName) : null,
					node.interfaces.stream().map(Type::getObjectType).toList(),
					node.access,
					fields,
					methods,
					ctors,
					node.visibleAnnotations,
					node.invisibleAnnotations,
					null
			);
		} catch(Exception e)
		{
			throw new ClassModelParseException(e);
		}
	}
	
	@Override
	public @Nullable Stream<String> commentLines()
	{
		return comment != null ? comment.lines() : null;
	}
}