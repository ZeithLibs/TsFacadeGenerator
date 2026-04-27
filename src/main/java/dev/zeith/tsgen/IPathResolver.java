package dev.zeith.tsgen;

import dev.zeith.tsgen.util.TypeUtil;
import org.objectweb.asm.Type;

import java.util.function.Function;

/**
 * Resolves the slash-separated path for a TypeScript file that will contain a given type.
 */
@FunctionalInterface
public interface IPathResolver
{
	/**
	 * Compacts the ts file count by merging classes from same package into single packaged file.
	 */
	IPathResolver FROM_PACKAGE = type -> TypeUtil.getPackagePath(type) + ".ts";
	
	/**
	 * Separate ts file per class file.
	 */
	IPathResolver FROM_CLASS_NAME = type -> type.getInternalName() + ".ts";
	
	String getPath(Type type);
	
	/**
	 * Simple adapter to allow working with type's internal names (java/lang/Object) instead of Types.
	 */
	static IPathResolver byInternalName(Function<String, String> resolver)
	{
		return type -> resolver.apply(type.getInternalName());
	}
}