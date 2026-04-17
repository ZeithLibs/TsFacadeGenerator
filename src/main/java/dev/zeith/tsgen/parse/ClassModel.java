package dev.zeith.tsgen.parse;

import dev.zeith.tsgen.exceptions.ClassModelParseException;
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
		List<AnnotationNode> invisibleAnnotations
)
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
			reader.accept(node, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
			
			if((node.access & Opcodes.ACC_PRIVATE) != 0)
				return null;
			
			List<FieldModel> fields = new ArrayList<>();
			List<MethodModel> methods = new ArrayList<>();
			List<ConstructorModel> ctors = new ArrayList<>();
			
			for(var n : node.fields)
			{
				boolean isPublic = (n.access & Opcodes.ACC_PUBLIC) != 0;
				if(!isPublic) continue;
				
				boolean isStatic = (n.access & Opcodes.ACC_STATIC) != 0;
				
				fields.add(new FieldModel(
						isStatic,
						n.name,
						NullAwareType.of(EnumNullability.of(n), Type.getType(n.desc), n.signature)
				));
			}
			
			for(var n : node.methods)
			{
				boolean isPublic = (n.access & Opcodes.ACC_PUBLIC) != 0;
				if(!isPublic) continue;
				
				var gen = SimpleGeneric.parseMethodDescSignature(n.signature);
				
				List<SimpleGeneric> parametrizedArgs = gen != null ? gen.parameters() : null;
				Type[] argTypes = Type.getArgumentTypes(n.desc);
				EnumNullability[] ns = EnumNullability.of(n);
				NullAwareType[] args = new NullAwareType[ns.length];
				for(int i = 0; i < argTypes.length; i++)
				{
					ParameterNode pn;
					var name = n.parameters != null && (pn = n.parameters.get(i)) != null && pn.name != null && !pn.name.isBlank() ? pn.name : "p" + i;
					args[i] = new NullAwareType(name, ns[i], argTypes[i], parametrizedArgs != null ? parametrizedArgs.get(i) : null);
				}
				
				if(n.name.startsWith("<"))
				{
					if(n.name.equals("<init>")) ctors.add(new ConstructorModel(args));
					continue;
				}
				
				methods.add(new MethodModel(
						n.access,
						n.name,
						gen != null ? gen.typeParameters() : null,
						new NullAwareType("ret", EnumNullability.ofReturnType(n), Type.getReturnType(n.desc), gen != null ? gen.returnType() : null),
						args
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
					node.invisibleAnnotations
			);
		} catch(Exception e)
		{
			throw new ClassModelParseException(e);
		}
	}
}