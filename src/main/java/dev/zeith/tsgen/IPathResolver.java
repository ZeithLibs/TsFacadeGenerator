package dev.zeith.tsgen;

import dev.zeith.tsgen.util.TypeUtil;
import org.objectweb.asm.Type;

import java.util.function.Function;

@FunctionalInterface
public interface IPathResolver
{
	IPathResolver FROM_PACKAGE = type -> TypeUtil.getPackagePath(type) + ".ts";
	IPathResolver FROM_CLASS_NAME = type -> type.getInternalName() + ".ts";
	
	String getPath(Type type);
	
	static IPathResolver byInternalName(Function<String, String> resolver)
	{
		return type -> resolver.apply(type.getInternalName());
	}
}