package dev.zeith.tsgen.parse.model;

import dev.zeith.tsgen.parse.NullAwareType;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public record FieldModel(
		boolean isStatic,
		String name,
		NullAwareType type,
		String comment
)
		implements IGeneralModel
{
	@Override
	public @Nullable Stream<String> commentLines()
	{
		return comment != null ? comment.lines() : null;
	}
}