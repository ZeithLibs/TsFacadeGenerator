package dev.zeith.tsgen.parse.src.type;

import dev.zeith.tsgen.parse.NullAwareType;
import dev.zeith.tsgen.parse.src.parse.ParseContext;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record ResolvedType(String simpleName, String internalName, String generics, ParseContext context)
		implements ISourceType
{
	@Override
	public ParseContext getContext()
	{
		return context;
	}
	
	@Override
	public String getSimpleName()
	{
		return simpleName;
	}
	
	@Override
	public @Nullable String getGenerics()
	{
		return generics;
	}
	
	@Override
	public Set<String> getPotentialInternalNames()
	{
		return Set.of(internalName);
	}
	
	@Override
	public boolean matches(NullAwareType type)
	{
		return this.internalName.equals(type.type().getInternalName());
	}
}