package dev.zeith.tsgen.parse.sig;

import java.util.List;

public record MethodGeneric(
		List<TypeParameter> typeParameters,
		List<SimpleGeneric> parameters,
		SimpleGeneric returnType
)
{
}