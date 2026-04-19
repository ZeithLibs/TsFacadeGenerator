package dev.zeith.tsgen.parse.src.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import dev.zeith.tsgen.parse.src.parse.ParseContext;
import dev.zeith.tsgen.parse.src.type.ISourceType;

import java.util.List;

public record SourceMethodModel(
		String commentBlock,
		String name,
		ISourceType returnType,
		List<SourceMethodParamModel> parameters,
		boolean isStatic,
		boolean isPublic
)
{
	public static SourceMethodModel parse(ParseContext ctx, MethodDeclaration md)
	{
		return new SourceMethodModel(
				ctx.parseComment(md),
				md.getNameAsString(),
				ctx.parseType(md.getType()),
				md.getParameters().stream().map(par -> SourceMethodParamModel.parse(ctx, par)).toList(),
				md.isStatic(),
				md.isPublic()
		);
	}
}