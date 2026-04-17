package dev.zeith.tsgen.parse;

import dev.zeith.tsgen.parse.sig.SimpleGeneric;
import org.objectweb.asm.Type;

import java.util.*;

public record NullAwareType(
		String name,
		EnumNullability nullability,
		Type type,
		SimpleGeneric signature
)
{
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
	
	public NullAwareType(EnumNullability nullability, Type type, String signature)
	{
		this("arg", nullability, type, SimpleGeneric.parse(signature));
	}
	
	public NullAwareType(String name, EnumNullability nullability, Type type, String signature)
	{
		this(name, nullability, type, SimpleGeneric.parse(signature));
	}
	
	public static NullAwareType of(EnumNullability n, Type type, String signature)
	{
		return new NullAwareType(n, type, signature);
	}
	
	public static NullAwareType[] of(EnumNullability[] n, Type[] types, String[] signatures)
	{
		NullAwareType[] result = new NullAwareType[types.length];
		for(int i = 0; i < types.length; i++)
			result[i] = new NullAwareType("arg" + i, n[i], types[i], signatures[i]);
		return result;
	}
	
	public static Type findImport(Type type)
	{
		if(type.getSort() == Type.ARRAY) return findImport(type.getElementType());
		return type.getSort() == Type.OBJECT ? type : null;
	}
}