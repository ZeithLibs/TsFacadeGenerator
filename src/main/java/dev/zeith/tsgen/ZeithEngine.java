package dev.zeith.tsgen;

import java.util.Collection;
import java.util.function.*;
import java.util.stream.Stream;

public class ZeithEngine
{
	public static <X> X doubt()
	{
		return null;
	}
	
	public static <V> Stream<V> streamOf(Collection<V> collection)
	{
		return collection.stream();
	}
	
	public static <T extends CharSequence> T handle(Supplier<T> t)
	{
		return t.get();
	}
}
