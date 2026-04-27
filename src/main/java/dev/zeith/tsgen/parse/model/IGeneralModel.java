package dev.zeith.tsgen.parse.model;

import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface IGeneralModel
{
	@Nullable
	Stream<String> commentLines();
}