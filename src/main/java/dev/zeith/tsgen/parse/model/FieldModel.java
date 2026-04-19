package dev.zeith.tsgen.parse.model;

import dev.zeith.tsgen.parse.NullAwareType;

public record FieldModel(
		boolean isStatic,
		String name,
		NullAwareType type
)
{
}