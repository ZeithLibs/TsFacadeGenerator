package dev.zeith.tsgen.util;

import org.objectweb.asm.Type;

public class TypeUtil
{
	public static String typeToTs(Type type)
	{
		return switch(type.getSort())
		{
			case Type.VOID -> "void";
			case Type.BOOLEAN -> "boolean";
			case Type.INT, Type.SHORT, Type.BYTE, Type.FLOAT, Type.DOUBLE, Type.LONG -> "number";
			case Type.ARRAY -> typeToTs(type.getElementType()) + "[]".repeat(type.getDimensions());
			case Type.OBJECT -> "L" + type.getInternalName() + ";";
			default -> "any";
		};
	}
	
	public static String relativePath(String ourFile, String targetFile)
	{
		String targetName = targetFile.substring(targetFile.lastIndexOf('/') + 1);
		String[] from = ourFile.substring(0, ourFile.lastIndexOf('/')).split("/");
		String[] to = targetFile.substring(0, targetFile.lastIndexOf('/')).split("/");
		
		// find common prefix length
		int common = 0;
		int maxCommon = Math.min(from.length, to.length);
		
		while(common < maxCommon && from[common].equals(to[common]))
			common++;
		
		StringBuilder sb = new StringBuilder();
		
		// go up from ourFile
		int upstream = Math.max(0, from.length - common);
		sb.append("../".repeat(upstream));
		if(upstream == 0) sb.append("./");
		
		// go down into "target"
		for(int i = common; i < to.length; i++)
			sb.append(to[i]).append("/");
		
		// add file name (JS/TS module name)
		sb.append(targetName);
		
		// normalize edge case: same package
		String result = sb.toString();
		return result.isEmpty() ? "./" + targetName : result;
	}
	
	public static String getPackagePath(Type type)
	{
		String path = type.getInternalName();
		int i = path.lastIndexOf('/');
		return path.substring(0, i);
	}
	
	public static String getSimpleName(Type name)
	{
		String clsn = name.getClassName();
		return clsn.substring(clsn.lastIndexOf('.') + 1);
	}
}