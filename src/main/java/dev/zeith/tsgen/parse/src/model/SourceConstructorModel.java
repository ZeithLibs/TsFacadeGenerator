package dev.zeith.tsgen.parse.src.model;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import dev.zeith.tsgen.parse.src.parse.ParseContext;

import java.util.List;

public record SourceConstructorModel(
		String commentBlock,
		List<SourceMethodParamModel> parameters,
		boolean isPublic
)
		implements IGeneralSourceModel
{
	public static SourceConstructorModel parse(ParseContext ctx, ConstructorDeclaration md)
	{
		return new SourceConstructorModel(
				ctx.parseComment(md),
				md.getParameters().stream().map(par -> SourceMethodParamModel.parse(ctx, par)).toList(),
				md.isPublic()
		);
	}
}