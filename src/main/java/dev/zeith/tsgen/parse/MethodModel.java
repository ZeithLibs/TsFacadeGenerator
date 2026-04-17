package dev.zeith.tsgen.parse;

import dev.zeith.tsgen.parse.sig.TypeParameter;
import org.objectweb.asm.Opcodes;

import java.util.List;

public record MethodModel(
		int access,
		String name,
		List<TypeParameter> typeParameters,
		NullAwareType returnType,
		NullAwareType[] args
)
{
	public boolean isStatic()
	{
		return (access & Opcodes.ACC_STATIC) != 0;
	}
	
	public boolean isFunctionalMethod()
	{
		return (access & Opcodes.ACC_ABSTRACT) != 0;
	}
}