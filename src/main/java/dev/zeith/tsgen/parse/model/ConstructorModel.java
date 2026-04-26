package dev.zeith.tsgen.parse.model;

import dev.zeith.tsgen.parse.NullAwareType;
import org.objectweb.asm.*;

public record ConstructorModel(
		int access,
		NullAwareType[] args
)
{
	public boolean isPublic()
	{
		return (access & Opcodes.ACC_PUBLIC) != 0;
	}
	
	public boolean isLastVararg()
	{
		return (access & Opcodes.ACC_VARARGS) != 0 && args.length > 0 && args[args.length - 1].type().getSort() == Type.ARRAY;
	}
}