package dev.zeith.tsgen.parse.sig;

import java.util.List;

public record TypeParameter(
		String name,
		List<SimpleGeneric> classBounds,
		List<SimpleGeneric> interfaceBounds
)
{
}