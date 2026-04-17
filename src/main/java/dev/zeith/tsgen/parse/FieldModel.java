package dev.zeith.tsgen.parse;

public record FieldModel(
		boolean isStatic,
		String name,
		NullAwareType type
)
{
}