package dev.zeith.tsgen.parse.src.type;

import dev.zeith.tsgen.parse.NullAwareType;
import dev.zeith.tsgen.parse.src.parse.ParseContext;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.*;

public record UndecidedType(String simpleName, String generics, ParseContext context)
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
		return internalNames().collect(Collectors.toSet());
	}
	
	@Override
	public boolean matches(NullAwareType type)
	{
		if(type.signature() != null && type.signature().paramRef() != null && type.signature().paramRef().equals(simpleName))
			return true;
		return internalNames().anyMatch(type.type().getInternalName()::equals);
	}
	
	Stream<String> internalNames()
	{
		return context.importedPackages.stream().map(p -> p + "/" + simpleName);
	}
}