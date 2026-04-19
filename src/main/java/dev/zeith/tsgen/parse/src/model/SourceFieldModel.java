package dev.zeith.tsgen.parse.src.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import dev.zeith.tsgen.parse.src.parse.ParseContext;
import dev.zeith.tsgen.parse.src.type.ISourceType;

import java.util.List;

public record SourceFieldModel(
		String commentBlock,
		String name,
		ISourceType type,
		boolean isStatic,
		boolean isPublic
)
{
	public static List<SourceFieldModel> parse(ParseContext ctx, FieldDeclaration decl)
	{
		String comment = ctx.parseComment(decl);
		boolean isStatic = decl.isStatic();
		boolean isPublic = decl.isPublic();
		return decl.getVariables()
				   .stream()
				   .map(vd ->
						   new SourceFieldModel(
								   comment,
								   vd.getNameAsString(),
								   ctx.parseType(vd.getType()),
								   isStatic, isPublic
						   )
				   )
				   .toList();
	}
}