package dev.zeith.tsgen.parse.model;

import dev.zeith.tsgen.parse.NullAwareType;
import dev.zeith.tsgen.parse.sig.TypeParameter;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.List;
import java.util.stream.Stream;

public record MethodModel(
		int access,
		String name,
		List<TypeParameter> typeParameters,
		NullAwareType returnType,
		NullAwareType[] args,
		String comment
)
		implements IGeneralModel
{
	public boolean isLastVararg()
	{
		return (access & Opcodes.ACC_VARARGS) != 0 && args.length > 0 && args[args.length - 1].type().getSort() == Type.ARRAY;
	}
	
	public boolean isPublic()
	{
		return (access & Opcodes.ACC_PUBLIC) != 0;
	}
	
	public boolean isStatic()
	{
		return (access & Opcodes.ACC_STATIC) != 0;
	}
	
	public boolean isBridge()
	{
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}
	
	public boolean isFunctionalMethod()
	{
		return (access & Opcodes.ACC_ABSTRACT) != 0;
	}
	
	@Override
	public @Nullable Stream<String> commentLines()
	{
		return comment != null ? comment.lines() : null;
	}
}