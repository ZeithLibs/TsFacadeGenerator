package dev.zeith.tsgen;

import dev.zeith.tsgen.imports.*;
import dev.zeith.tsgen.parse.*;
import dev.zeith.tsgen.parse.sig.*;
import dev.zeith.tsgen.util.TypeUtil;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class TypeScriptGenerator
{
	public static final Map<String, String> TS_ALTS = Map.of(
			"java/lang/Object", "object",
			"java/lang/String", "string",
			"java/lang/Void", "void",
			"java/util/Map", "Map",
			"java/util/Set", "Set"
	);
	
	protected final ClassModel model;
	
	protected String newline = "\n";
	protected String indent = "\t";
	protected IImportModel importModel = RequireImportModel.INSTANCE;
	protected final Set<Type> imports = new HashSet<>();
	
	public TypeScriptGenerator(ClassModel model)
	{
		this.model = model;
	}
	
	public TypeScriptGenerator withNewline(String newline)
	{
		this.newline = Objects.requireNonNull(newline, "newline");
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
	{
		imports.clear();
		
		final int startPos = out.length(); // saved for imports
		
		out.append(newline).append(newline);
		
		generateDeclare(out);
		out.append(newline).append(newline);
		generateInterface(out);
		
		if(genImports)
			out.insert(startPos, generateImports());
	}
	
	protected void generateDeclare(StringBuilder out)
	{
		out.append("export declare class ").append(model.getSimpleName());
		appendClassGenerics(out);
		out.append(" {");
		
		boolean needNewLine = false;
		
		for(ConstructorModel ctor : model.constructors())
		{
			needNewLine = true;
			out.append(newline).append(indent)
			   .append("constructor(")
			   .append(Arrays
					   .stream(ctor.args())
					   .map(t -> t.name() + ": " + mapType(t))
					   .collect(Collectors.joining(", "))
			   )
			   .append(");");
		}
		if(needNewLine)
		{
			out.append(newline);
			needNewLine = false;
		}
		
		for(var field : model.fields())
		{
			if(!field.isStatic()) continue;
			appendField(out.append(newline).append(indent).append("static "), field);
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
			appendMethod(out.append(newline).append(indent).append("static "), method);
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
		
		ClassGeneric generic = model.generic();
		if(generic != null)
		{
			List<SimpleGeneric> gens = new ArrayList<>();
			gens.add(generic.superClass());
			gens.addAll(generic.interfaces());
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
				List<String> lst = new ArrayList<>();
				for(var gen : allExtends) lst.add(mapType(false, gen, null));
				lst.remove("Ljava/lang/Object;");
				if(!lst.isEmpty())
					out.append(" extends ").append(remapType(String.join(", ", lst)));
			}
		}
		
		out.append(" {");
		boolean needNewLine = false;
		
		if(model.isFunctionalInterface())
		{
			// find functional method
			
			List<MethodModel> fnMethods = model.methods().stream().filter(MethodModel::isFunctionalMethod).toList();
			if(fnMethods.size() == 1)
			{
				var method = fnMethods.getFirst();
				appendRenamedMethod(out.append(newline).append(indent), method, "");
				needNewLine = true;
			}
		}
		
		for(var field : model.fields())
		{
			if(field.isStatic()) continue;
			appendField(out.append(newline).append(indent), field);
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
			appendMethod(out.append(newline).append(indent), method);
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
	
	protected void addGenericExtends(StringBuilder sb, List<SimpleGeneric> extensions, String delim)
	{
		List<String> lst = new ArrayList<>();
		for(SimpleGeneric gen : extensions) lst.add(mapType(false, gen.base(), gen));
		lst.remove("Ljava/lang/Object;");
		if(!lst.isEmpty()) sb.append(" extends ").append(remapType(String.join(delim, lst)));
	}
	
	protected void appendField(StringBuilder sb, FieldModel field)
	{
		sb.append(field.name())
		  .append(": ")
		  .append(mapType(field.type()))
		  .append(";");
	}
	
	protected void appendMethod(StringBuilder sb, MethodModel method)
	{
		appendRenamedMethod(sb, method, method.name());
	}
	
	protected void appendRenamedMethod(StringBuilder sb, MethodModel method, String name)
	{
		sb.append(name);
		
		// type args
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
		
		sb.append("(")
		  .append(Arrays.stream(method.args()).map(a -> a.name() + ": " + mapType(a)).collect(Collectors.joining(", ")))
		  .append("): ")
		  .append(mapType(method.returnType()))
		  .append(";");
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
