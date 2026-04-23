package dev.zeith.tsgen;

import dev.zeith.tsgen.exceptions.TypeScriptGenException;
import dev.zeith.tsgen.imports.*;
import dev.zeith.tsgen.parse.NullAwareType;
import dev.zeith.tsgen.parse.model.*;
import dev.zeith.tsgen.parse.sig.*;
import dev.zeith.tsgen.parse.src.model.*;
import dev.zeith.tsgen.util.TypeUtil;
import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.*;
import java.util.stream.Collectors;

public class TypeScriptGenerator
{
	// Predicate for matching field/method names.
	public static Predicate<String> TS_IDENTIFIER = Pattern.compile("^[\\p{L}_$][\\p{L}\\p{N}_$]*$").asMatchPredicate();
	
	/**
	 * Alternative TS-friendly names
	 */
	public static final Map<String, String> TS_ALTS = Map.of(
			"java/lang/Object", "object",
			"java/lang/String", "string",
			"java/lang/Void", "void"
//			"java/util/Map", "Map",
//			"java/util/Set", "Set"
	);
	
	protected final ClassModel model;
	protected final @Nullable SourceClassModel sourceModel;
	
	public String newline = "\n";
	public String indent = "\t";
	public IImportModel importModel = RequireImportModel.INSTANCE;
	public GeneratorExceptionHandler exceptionHandler = GeneratorExceptionHandler.GLOBAL_FAIL;
	public final Set<Type> imports = new HashSet<>();
	
	public TypeScriptGenerator(@NotNull ClassModel model, @Nullable SourceClassModel sourceModel)
	{
		this.model = model;
		this.sourceModel = sourceModel;
	}
	
	public TypeScriptGenerator withNewline(String newline)
	{
		this.newline = Objects.requireNonNull(newline, "newline");
		return this;
	}
	
	public TypeScriptGenerator withExceptionHandler(GeneratorExceptionHandler exceptionHandler)
	{
		this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		return this;
	}
	
	public TypeScriptGenerator withIndentation(String indentation)
	{
		this.indent = Objects.requireNonNull(indentation, "indentation");
		return this;
	}
	
	public TypeScriptGenerator withImportModel(IImportModel importModel)
	{
		this.importModel = Objects.requireNonNull(importModel, "importModel");
		return this;
	}
	
	public void generate(StringBuilder out, boolean genImports)
			throws TypeScriptGenException
	{
		imports.clear();
		
		final int startPos = out.length(); // saved for imports
		
		try
		{
			out.append(newline);
			
			generateDeclare(out);
			out.append(newline).append(newline);
			generateInterface(out);
			
			if(genImports)
			{
				out.insert(startPos, newline.repeat(2));
				out.insert(startPos, generateImports());
			}
		} catch(Exception e)
		{
			throw new TypeScriptGenException(e);
		}
	}
	
	protected void appendComment(StringBuilder out, int spacingAbove, int indent, @Nullable String commentBlock)
	{
		if(commentBlock == null) return;
		String indentedNL = newline + this.indent.repeat(indent);
		out.append(indentedNL.repeat(spacingAbove))
		   .append(commentBlock
				   .lines()
				   .map(String::trim)
				   .map(s -> s.startsWith("*") ? " " + s : s)
				   .collect(Collectors.joining(indentedNL))
		   );
	}
	
	protected void generateDeclare(StringBuilder out)
	{
		if(sourceModel != null) appendComment(out, 0, 0, sourceModel.commentBlock());
		out.append(newline).append("export declare class ").append(model.getSimpleName());
		appendClassGenerics(out);
		appendClassExtensions(out, true);
		out.append(" {");
		
		boolean needNewLine = false;
		
		for(ConstructorModel ctor : model.constructors())
		{
			List<String> parameterNames = null;
			if(sourceModel != null)
			{
				SourceConstructorModel scm = sourceModel.findSourceMethod(ctor);
				if(scm != null)
				{
					appendComment(out, 1, 1, scm.commentBlock());
					parameterNames = scm.parameters().stream().map(SourceMethodParamModel::name).toList();
				}
			}
			
			needNewLine = true;
			out.append(newline).append(indent)
			   .append("constructor(");
			
			if(parameterNames != null)
			{
				List<String> pnames = new ArrayList<>();
				var args = ctor.args();
				for(int i = 0; i < args.length; i++)
				{
					NullAwareType a = args[i];
					pnames.add(parameterNames.get(i) + ": " + mapType(a));
				}
				out.append(String.join(", ", pnames));
			} else
				out.append(Arrays.stream(ctor.args()).map(t -> t.name() + ": " + mapType(t)).collect(Collectors.joining(", ")));
			
			out.append(");");
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		for(var field : model.fields())
		{
			if(!field.isStatic()) continue;
			appendField(newline + indent + "static ", out, field);
			needNewLine = true;
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		for(var method : model.methods())
		{
			if(!method.isStatic()) continue;
			appendMethod(newline + indent + "static ", out, method);
			needNewLine = true;
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		out.append("}");
	}
	
	protected void generateInterface(StringBuilder out)
	{
		out.append("export interface ").append(model.getSimpleName());
		appendClassGenerics(out);
		appendClassExtensions(out, false);
		out.append(" {");
		
		boolean needNewLine = false;
		
		if(model.isInterface())
		{
			// find functional method
			List<MethodModel> fnMethods = model.methods().stream().filter(MethodModel::isFunctionalMethod).toList();
			if(fnMethods.size() == 1)
			{
				var method = fnMethods.get(0);
				appendRenamedMethod(newline + indent, out, method, "");
				needNewLine = true;
			}
		}
		
		for(var field : model.fields())
		{
			if(field.isStatic()) continue;
			appendField(newline + indent, out, field);
			needNewLine = true;
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		for(var method : model.methods())
		{
			if(method.isStatic()) continue;
			appendMethod(newline + indent, out, method);
			needNewLine = true;
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		out.append("}");
	}
	
	protected void appendClassGenerics(StringBuilder sb)
	{
		ClassGeneric generic = model.generic();
		if(generic != null && generic.typeParameters() != null && !generic.typeParameters().isEmpty())
		{
			sb.append("<");
			sb.append(generic.typeParameters().stream().map(t ->
			{
				StringBuilder val = new StringBuilder(t.name());
				List<SimpleGeneric> gens = new ArrayList<>();
				gens.addAll(t.classBounds());
				gens.addAll(t.interfaceBounds());
				addGenericExtends(val, gens, " & ");
				return val.toString();
			}).collect(Collectors.joining(", ")));
			sb.append(">");
		}
	}
	
	protected void appendClassExtensions(StringBuilder out, boolean pickFirst)
	{
		ClassGeneric generic = model.generic();
		if(generic != null)
		{
			List<SimpleGeneric> gens = new ArrayList<>();
			gens.add(generic.superClass());
			gens.addAll(generic.interfaces());
			gens.removeIf(Objects::isNull);
			
			if(pickFirst && gens.size() > 1)
			{
				var f = gens.get(0);
				gens.clear();
				gens.add(f);
			}
			
			if(!gens.isEmpty())
				addGenericExtends(out, gens, ", ");
		} else
		{
			List<Type> allExtends = new ArrayList<>();
			Type superType = model.superName();
			if(superType != null) allExtends.add(superType);
			allExtends.addAll(model.interfaces());
			if(!allExtends.isEmpty())
			{
				List<String> gens = new ArrayList<>();
				for(var gen : allExtends) gens.add(mapType(false, gen, null));
				gens.remove("Ljava/lang/Object;");
				
				if(pickFirst && gens.size() > 1)
				{
					var f = gens.get(0);
					gens.clear();
					gens.add(f);
				}
				
				if(!gens.isEmpty())
					out.append(" extends ").append(remapType(String.join(", ", gens)));
			}
		}
	}
	
	protected void addGenericExtends(StringBuilder sb, List<SimpleGeneric> extensions, String delim)
	{
		List<String> lst = new ArrayList<>();
		for(SimpleGeneric gen : extensions) lst.add(mapType(false, gen.base(), gen));
		lst.remove("Ljava/lang/Object;");
		if(!lst.isEmpty()) sb.append(" extends ").append(remapType(String.join(delim, lst)));
	}
	
	protected void appendField(String prefix, StringBuilder output, FieldModel field)
	{
		// Skip invalid field names
		if(!TS_IDENTIFIER.test(field.name())) return;
		
		StringBuilder sb = new StringBuilder();
		
		if(sourceModel != null)
		{
			SourceFieldModel sfm = sourceModel.findSourceField(field);
			if(sfm != null) appendComment(sb, 1, 1, sfm.commentBlock());
		}
		
		try
		{
			if(prefix != null) sb.append(prefix);
			sb.append(field.name())
			  .append(": ")
			  .append(mapType(field.type()))
			  .append(";");
		} catch(RuntimeException e)
		{
			if(handleRuntimeException(e).shouldSkip())
				return;
		}
		
		output.append(sb);
	}
	
	protected void appendMethod(String prefix, StringBuilder sb, MethodModel method)
	{
		appendRenamedMethod(prefix, sb, method, method.name());
	}
	
	protected void appendRenamedMethod(String prefix, StringBuilder output, MethodModel method, String name)
	{
		// Skip invalid method names
		if(method.isBridge() || (!name.isEmpty() && !TS_IDENTIFIER.test(name))) return;
		
		StringBuilder sb = new StringBuilder();
		
		List<String> parameterNames = null;
		if(sourceModel != null)
		{
			SourceMethodModel smm = sourceModel.findSourceMethod(method);
			if(smm != null)
			{
				appendComment(sb, 1, 1, smm.commentBlock());
				parameterNames = smm.parameters().stream().map(SourceMethodParamModel::name).toList();
			}
		}
		
		try
		{
			if(prefix != null) sb.append(prefix);
			sb.append(name);
			
			// returnType args
			var pars = method.typeParameters();
			if(pars != null && !pars.isEmpty())
			{
				sb.append("<");
				sb.append(pars.stream().map(t ->
				{
					StringBuilder val = new StringBuilder(t.name());
					List<SimpleGeneric> gens = new ArrayList<>();
					gens.addAll(t.classBounds());
					gens.addAll(t.interfaceBounds());
					addGenericExtends(val, gens, " & ");
					return val.toString();
				}).collect(Collectors.joining(", ")));
				sb.append(">");
			}
			
			sb.append("(");
			
			List<String> pnames = new ArrayList<>();
			var args = method.args();
			for(int i = 0; i < args.length; i++)
			{
				NullAwareType a = args[i];
				String pref = "";
				if(i == args.length - 1 && method.isLastVararg()) pref = "...";
				pnames.add(pref + (parameterNames != null && a.decodedName() == null ? parameterNames.get(i) : a.name()) + ": " + mapType(a));
			}
			sb.append(String.join(", ", pnames));
			
			sb.append("): ").append(mapType(method.returnType())).append(";");
		} catch(RuntimeException e)
		{
			if(handleRuntimeException(e).shouldSkip())
				return;
		}
		
		output.append(sb);
	}
	
	protected GeneratorExceptionHandler handleRuntimeException(RuntimeException ex)
	{
		if(exceptionHandler == GeneratorExceptionHandler.GLOBAL_FAIL)
			throw ex;
		return exceptionHandler;
	}
	
	public String generateImports()
	{
		return importModel.generateImports(newline, model.name(), imports.stream().sorted(IImportModel.IMPORT_COMPARATOR).toList());
	}
	
	protected static final Pattern TYPE_FINDER = Pattern.compile("L[^;]+;");
	
	protected String mapType(NullAwareType type)
	{
		addImports(type.getImports());
		return remapType(
				mapType(type.nullability().isNullable(), type.type(), type.signature())
		);
	}
	
	protected String remapType(String t)
	{
		Matcher m = TYPE_FINDER.matcher(t);
		while(m.find())
		{
			Type desc = Type.getType(m.group());
			String replacement = TS_ALTS.get(desc.getInternalName());
			if(replacement == null) replacement = TypeUtil.getSimpleName(desc);
			if(replacement.isEmpty()) continue;
			t = t.replace(m.group(), replacement);
		}
		return t;
	}
	
	public void addImports(Collection<Type> imports)
	{
		this.imports.addAll(imports.stream().filter(t -> !TS_ALTS.containsKey(t.getInternalName())).toList());
	}
	
	protected String mapType(boolean allowUndefined, Type type, SimpleGeneric signature)
	{
		if(type == null && signature != null && signature.paramRef() != null)
			return signature.paramRef();
		
		return switch(type.getSort())
		{
			case Type.VOID -> "void";
			case Type.BOOLEAN -> "boolean";
			case Type.INT, Type.SHORT, Type.BYTE, Type.FLOAT, Type.DOUBLE, Type.LONG -> "number";
			case Type.ARRAY -> mapType(false, type.getElementType(), signature) + "[]".repeat(type.getDimensions());
			case Type.OBJECT ->
			{
				if(signature != null) addImports(signature.getImports());
				else addImports(List.of(type));
				
				String val = "L" + type.getInternalName() + ";";
				
				if(signature != null)
				{
					yield signature.withDimensions(0).getTsSimpleName();
				}
				
				yield val;
			}
			default -> "any";
		} + (allowUndefined && type.getSort() != Type.VOID ? " | undefined" : "");
	}
}
