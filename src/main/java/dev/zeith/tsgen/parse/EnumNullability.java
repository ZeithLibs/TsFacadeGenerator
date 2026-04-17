package dev.zeith.tsgen.parse;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Stream;

public enum EnumNullability
{
	UNDEFINED,
	NOT_NULL,
	NULLABLE;
	
	public static final EnumNullability[] ZERO_ARRAY = new EnumNullability[0];
	
	public static EnumNullability of(FieldNode node)
	{
		return findNullability(Stream.concat(
				lstStream(node.visibleAnnotations),
				lstStream(node.invisibleAnnotations)
		));
	}
	
	public static EnumNullability ofReturnType(MethodNode node)
	{
		return findNullability(Stream.concat(
				lstStream(node.visibleAnnotations),
				lstStream(node.invisibleAnnotations)
		));
	}
	
	public static EnumNullability[] of(MethodNode node)
	{
		int argumentCount = Type.getArgumentCount(node.desc);
		if(argumentCount <= 0) return ZERO_ARRAY;
		
		EnumNullability[] nulls = new EnumNullability[argumentCount];
		
		List<AnnotationNode>[] invis = node.invisibleParameterAnnotations;
		List<AnnotationNode>[] vis = node.visibleParameterAnnotations;
		if(invis == null) invis = new List[nulls.length];
		if(vis == null) vis = new List[nulls.length];
		
		for(int i = 0; i < nulls.length; i++)
			nulls[i] = findNullability(Stream.concat(
					lstStream(invis[i]),
					lstStream(vis[i])
			));
		return nulls;
	}
	
	public static EnumNullability findNullability(Stream<AnnotationNode> nodes)
	{
		return nodes.map(node ->
		{
			var d = node.desc.toLowerCase(Locale.ROOT);
			return d.contains("nonnull") || d.contains("notnull")
				   ? NOT_NULL
				   : d.contains("nullable")
					 ? NULLABLE
					 : UNDEFINED;
		}).filter(n -> n != UNDEFINED).findFirst().orElse(UNDEFINED);
	}
	
	public static <T> Stream<T> lstStream(List<T> list)
	{
		return list != null ? list.stream() : Stream.empty();
	}
	
	public boolean isNullable()
	{
		return NULLABLE == this;
	}
}