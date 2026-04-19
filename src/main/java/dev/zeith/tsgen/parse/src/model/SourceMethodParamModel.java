package dev.zeith.tsgen.parse.src.model;

import com.github.javaparser.ast.body.Parameter;
import dev.zeith.tsgen.parse.src.parse.ParseContext;
import dev.zeith.tsgen.parse.src.type.ISourceType;

public record SourceMethodParamModel(
		String name,
		ISourceType type
)
{
	public static SourceMethodParamModel parse(ParseContext ctx, Parameter par)
	{
		return new SourceMethodParamModel(
				par.getNameAsString(),
				ctx.parseType(par.getType())
		);
	}
}