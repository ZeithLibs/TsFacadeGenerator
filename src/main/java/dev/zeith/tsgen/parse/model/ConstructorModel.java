package dev.zeith.tsgen.parse.model;

import dev.zeith.tsgen.parse.NullAwareType;
import org.objectweb.asm.Opcodes;

public record ConstructorModel(
		int access,
		NullAwareType[] args
)
{
	public boolean isPublic()
	{
		return (access & Opcodes.ACC_PUBLIC) != 0;
	}
}