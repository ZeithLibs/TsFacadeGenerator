package dev.zeith.tsgen;

import dev.zeith.tsgen.api.*;
import dev.zeith.tsgen.exceptions.TypeScriptGenException;
import dev.zeith.tsgen.imports.IImportModel;
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
import java.util.stream.*;

public class TypeScriptGenerator
{
	// Predicate for matching field/method names.
	public static Predicate<String> TS_IDENTIFIER = Pattern.compile("^[\\p{L}_$][\\p{L}\\p{N}_$]*[?]?$").asMatchPredicate();
	
	/**
	 * Alternative TS-friendly names
	 */
	public static final Map<String, String> TS_ALTS = Map.of(
			"java/lang/Object", "object",
			"java/lang/String", "string",
			"java/lang/Void", "void",
			"java/lang/Boolean", "boolean"
//			"java/util/Map", "Map",
//			"java/util/Set", "Set"
	);
	
	private static final Set<String> JS_RESERVED = Set.of(
			"break", "case", "catch", "class", "const", "continue", "debugger", "default",
			"delete", "do", "else", "export", "extends", "finally", "for", "function",
			"if", "import", "in", "instanceof", "let", "new", "return", "super", "switch",
			"this", "throw", "try", "typeof", "var", "void", "while", "with", "yield"
	);
	
	protected final @NotNull ClassModel model;
	protected final @NotNull TSGenSettings settings;
	protected final @Nullable SourceClassModel sourceModel;
	
	public final Set<Type> imports = new HashSet<>();
	
	protected final @NotNull List<ITypeExtension> typeExtensions;
	
	public TypeScriptGenerator(@NotNull TSGenSettings settings, @NotNull ClassModel model)
	{
		this(settings, model, null);
	}
	
	public TypeScriptGenerator(@NotNull TSGenSettings settings, @NotNull ClassModel model, @Nullable SourceClassModel sourceModel)
	{
		this(settings, model, sourceModel, IGenerationExtension.DEFAULT_ENABLED);
	}
	
	public TypeScriptGenerator(@NotNull TSGenSettings settings, @NotNull ClassModel model, @Nullable SourceClassModel sourceModel, @NotNull Predicate<IGenerationExtension> enabledExtensions)
	{
		this(settings, model, sourceModel, ITypeExtension.gather(enabledExtensions, model, sourceModel));
	}
	
	public TypeScriptGenerator(@NotNull TSGenSettings settings, @NotNull ClassModel model, @Nullable SourceClassModel sourceModel, @NotNull List<ITypeExtension> typeExtensions)
	{
		this.settings = settings;
		this.model = model;
		this.sourceModel = sourceModel;
		this.typeExtensions = typeExtensions;
	}
	
	protected Iterable<ConstructorModel> constructors()
	{
		return () -> Stream.concat(ITypeExtension.getExtraConstructors(this.typeExtensions), this.model.constructors().stream()).iterator();
	}
	
	protected Iterable<FieldModel> fields()
	{
		return () -> Stream.concat(ITypeExtension.getExtraFields(this.typeExtensions), this.model.fields().stream()).iterator();
	}
	
	protected Iterable<MethodModel> methods()
	{
		return () -> Stream.concat(ITypeExtension.getExtraMethods(this.typeExtensions), this.model.methods().stream()).iterator();
	}
	
	public void generate(StringBuilder out, boolean genImports)
			throws TypeScriptGenException
	{
		imports.clear();
		
		final int startPos = out.length(); // saved for imports
		final var newline = settings.newline;
		
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
	
	protected void appendComment(StringBuilder out, int spacingAbove, int indent, IGeneralModel model, @Nullable IGeneralSourceModel srcModel)
	{
		var lines = model.commentLines();
		if(lines == null && srcModel != null)
		{
			var com = srcModel.commentBlock();
			if(com != null) lines = com.lines();
		} else if(lines != null)
		{
			lines = Stream.concat(Stream.of("/**"), lines.map(ln -> "* " + ln));
			lines = Stream.concat(lines, Stream.of("*/"));
		}
		
		if(lines == null) return;
		
		final var newline = settings.newline;
		String indentedNL = newline + settings.indent.repeat(indent);
		
		out.append(indentedNL.repeat(spacingAbove))
		   .append(lines
				   .map(String::trim)
				   .map(s -> s.startsWith("*") ? " " + s : s)
				   .collect(Collectors.joining(indentedNL))
		   );
	}
	
	protected void generateDeclare(StringBuilder out)
	{
		final var newline = settings.newline;
		final var indent = settings.indent;
		
		appendComment(out, 0, 0, model, sourceModel);
		out.append(newline).append("export declare class ").append(model.getSimpleName());
		appendClassGenerics(out);
		appendClassExtensions(out, true);
		out.append(" {");
		
		boolean needNewLine = false;
		
		for(ConstructorModel ctor : constructors())
		{
			List<String> parameterNames = null;
			SourceConstructorModel scm = null;
			if(sourceModel != null)
			{
				scm = sourceModel.findSourceCtor(ctor);
				if(scm != null) parameterNames = scm.parameters().stream().map(SourceMethodParamModel::name).toList();
			}
			appendComment(out, 1, 1, ctor, scm);
			
			needNewLine = true;
			out.append(newline).append(indent)
			   .append("constructor(");
			
			List<String> pnames = new ArrayList<>();
			var args = ctor.args();
			for(int i = 0; i < args.length; i++)
			{
				NullAwareType a = args[i];
				String pref = "";
				if(i == args.length - 1 && ctor.isLastVararg() && settings.enableVarargs) pref = "...";
				String param = parameterNames != null && a.decodedName() == null ? parameterNames.get(i) : a.name();
				pnames.add(pref + paramName(i, param) + ": " + mapType(a));
			}
			out.append(String.join(", ", pnames));
			
			out.append(");");
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		for(var field : fields())
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
		
		for(var method : methods())
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
		final var newline = settings.newline;
		final var indent = settings.indent;
		
		out.append("export interface ").append(model.getSimpleName());
		appendClassGenerics(out);
		appendClassExtensions(out, false);
		out.append(" {");
		
		boolean needNewLine = false;
		boolean hasLambdaMethod = false;
		if(model.isFunctionalInterface())
		{
			// find functional method
			List<MethodModel> fnMethods = model.methods().stream().filter(MethodModel::isFunctionalMethod).toList();
			if(fnMethods.size() == 1)
			{
				var method = fnMethods.get(0);
				appendRenamedMethod(newline + indent, out, method, "");
				needNewLine = true;
				hasLambdaMethod = true;
			}
		}
		
		for(var field : fields())
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
		
		for(var method : methods())
		{
			if(method.isStatic()) continue;
			appendRenamedMethod(newline + indent, out, method, hasLambdaMethod ? method.name() + "?" : method.name());
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
		
		SourceFieldModel sfm = sourceModel != null ? sourceModel.findSourceField(field) : null;
		appendComment(sb, 1, 1, field, sfm);
		
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
		SourceMethodModel smm = null;
		if(sourceModel != null)
		{
			smm = sourceModel.findSourceMethod(method);
			if(smm != null) parameterNames = smm.parameters().stream().map(SourceMethodParamModel::name).toList();
		}
		appendComment(sb, 1, 1, method, smm);
		
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
				if(i == args.length - 1 && method.isLastVararg() && settings.enableVarargs) pref = "...";
				String param = parameterNames != null && a.decodedName() == null ? parameterNames.get(i) : a.name();
				pnames.add(pref + paramName(i, param) + ": " + mapType(a));
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
	
	protected String paramName(int index, String parName)
	{
		if(JS_RESERVED.contains(parName))
			return Character.toUpperCase(parName.charAt(0)) + parName.substring(1);
		return parName;
	}
	
	protected GeneratorExceptionHandler handleRuntimeException(RuntimeException ex)
	{
		if(settings.exceptionHandler == GeneratorExceptionHandler.GLOBAL_FAIL)
			throw ex;
		return settings.exceptionHandler;
	}
	
	public String generateImports()
	{
		return settings.importModel.generateImports(settings.newline, model.name(), imports.stream().sorted(IImportModel.IMPORT_COMPARATOR).toList());
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
