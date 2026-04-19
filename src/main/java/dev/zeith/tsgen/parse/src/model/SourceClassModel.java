package dev.zeith.tsgen.parse.src.model;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import dev.zeith.tsgen.parse.*;
import dev.zeith.tsgen.parse.model.*;
import dev.zeith.tsgen.parse.src.parse.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record SourceClassModel(
		@Nullable String commentBlock,
		String name,
		List<SourceConstructorModel> constructors,
		List<SourceFieldModel> fields,
		List<SourceMethodModel> methods
)
{
	public SourceFieldModel findSourceField(FieldModel b)
	{
		for(var e : fields)
		{
			if(e.isStatic() == b.isStatic() && e.name().equals(b.name())
					&& e.type().matches(b.type().type().getInternalName())
			) return e;
		}
		return null;
	}
	
	public SourceMethodModel findSourceMethod(MethodModel b)
	{
		for(var e : methods)
		{
			if(e.isStatic() == b.isStatic() && e.name().equals(b.name())
					&& e.returnType().matches(b.returnType().type().getInternalName())
					&& matchSignature(b.args(), e.parameters())
			) return e;
		}
		return null;
	}
	
	public SourceConstructorModel findSourceMethod(ConstructorModel b)
	{
		for(var e : constructors)
			if(e.isPublic() == b.isPublic() && matchSignature(b.args(), e.parameters()))
				return e;
		return null;
	}
	
	private boolean matchSignature(NullAwareType[] types, List<SourceMethodParamModel> params)
	{
		if(types.length != params.size()) return false;
		for(int i = 0; i < types.length; i++)
			if(!params.get(i).type().matches(types[i].type().getInternalName()))
				return false;
		return true;
	}
	
	@Nullable
	public static Map<String, SourceClassModel> parse(SourceParser parser, String code)
	{
		CompilationUnit compilationUnit = parser.parse(code);
		NodeList<TypeDeclaration<?>> units = compilationUnit.getTypes();
		if(units == null) return null;
		
		ParseContext ctx = new ParseContext(compilationUnit.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse(null));
		for(ImportDeclaration id : compilationUnit.getImports())
		{
			if(id.isAsterisk())
				ctx.registerWildcardImport(id.getNameAsString());
			else
				ctx.registerClassImport(id.getNameAsString());
		}
		
		Map<String, SourceClassModel> result = new HashMap<>();
		for(TypeDeclaration<?> unit : units)
		{
			var s = parseSingle(ctx, unit);
			if(s != null) result.put(unit.getNameAsString(), s);
		}
		
		return result;
	}
	
	private static SourceClassModel parseSingle(ParseContext ctx, TypeDeclaration<?> type)
	{
		String fqn = type.getFullyQualifiedName().orElse(null);
		if(fqn == null) return null;
		
		List<SourceConstructorModel> constructors = new ArrayList<>();
		List<SourceFieldModel> fields = new ArrayList<>();
		List<SourceMethodModel> methods = new ArrayList<>();
		
		for(BodyDeclaration<?> member : type.getMembers())
		{
			if(member instanceof FieldDeclaration fd)
			{
				fields.addAll(SourceFieldModel.parse(ctx, fd));
			} else if(member instanceof MethodDeclaration md)
			{
				methods.add(SourceMethodModel.parse(ctx, md));
			} else if(member instanceof ConstructorDeclaration cd)
			{
				constructors.add(SourceConstructorModel.parse(ctx, cd));
			}
		}
		
		return new SourceClassModel(
				ctx.parseComment(type),
				fqn,
				constructors,
				fields,
				methods
		);
	}
}