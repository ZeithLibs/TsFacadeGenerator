package dev.zeith.tsgen.parse;

import dev.zeith.tsgen.parse.sig.SimpleGeneric;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

public record NullAwareType(
		int index,
		@Nullable String decodedName,
		EnumNullability nullability,
		Type type,
		SimpleGeneric signature
)
{
	public static final Type JAVA_LANG_OBJECT = Type.getObjectType("java/lang/Object");
	public static final Type JAVA_LANG_CLASS = Type.getObjectType("java/lang/Class");
	
	public String name()
	{
		return decodedName != null ? decodedName : "p" + index;
	}
	
	public Set<Type> getImports()
	{
		Set<Type> list = new HashSet<>();
		
		var i = findImport(type);
		if(i != null) list.add(i);
		
		if(signature != null)
		{
			signature.visitTypes(t ->
			{
				var f = findImport(t);
				if(f != null) list.add(f);
			});
		}
		
		return list;
	}
	
	public NullAwareType(int index, EnumNullability nullability, Type type, String signature)
	{
		this(index, "arg", nullability, type, SimpleGeneric.parse(signature));
	}
	
	public NullAwareType(int index, String name, EnumNullability nullability, Type type, String signature)
	{
		this(index, name, nullability, type, SimpleGeneric.parse(signature));
	}
	
	public static NullAwareType of(int index, EnumNullability n, Type type, String signature)
	{
		return new NullAwareType(index, n, type, signature);
	}
	
	public static NullAwareType[] of(EnumNullability[] n, Type[] types, String[] signatures)
	{
		NullAwareType[] result = new NullAwareType[types.length];
		for(int i = 0; i < types.length; i++)
			result[i] = new NullAwareType(i, "arg" + i, n[i], types[i], signatures[i]);
		return result;
	}
	
	public static Type findImport(Type type)
	{
		if(type.getSort() == Type.ARRAY)
		{
			Type elemType = null;
			try
			{
				elemType = type.getElementType();
			} catch(StringIndexOutOfBoundsException e)
			{
				// Kotlin moment......
				elemType = JAVA_LANG_OBJECT;
			}
			return findImport(elemType);
		}
		return type.getSort() == Type.OBJECT ? type : null;
	}
}