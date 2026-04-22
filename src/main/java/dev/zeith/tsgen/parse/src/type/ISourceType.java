package dev.zeith.tsgen.parse.src.type;

import dev.zeith.tsgen.parse.NullAwareType;
import dev.zeith.tsgen.parse.src.parse.ParseContext;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ISourceType
{
	ParseContext getContext();
	
	String getSimpleName();
	
	@Nullable
	String getGenerics();
	
	Set<String> getPotentialInternalNames();
	
	boolean matches(NullAwareType type);
}