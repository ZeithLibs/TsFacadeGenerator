package dev.zeith.tsgen.parse;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

public interface IClassFileVisitor<X extends Exception>
{
	void visit(String path, InputStream input)
			throws IOException, X;
	
	static <X extends Exception> void visit(File jarOrDir, IClassFileVisitor<X> visitor)
			throws IOException, X
	{
		if(!jarOrDir.exists()) return;
		if(jarOrDir.isDirectory()) visitDir(jarOrDir, visitor);
		else if(jarOrDir.isFile())
		{
			String name = jarOrDir.getName().toLowerCase(Locale.ROOT);
			if(name.endsWith(".jar") || name.endsWith(".zip"))
				visitZipFile(jarOrDir, visitor);
		}
	}
	
	private static <X extends Exception> void visitDir(File dir, IClassFileVisitor<X> visitor)
			throws IOException, X
	{
		var root = dir.toPath();
		try(var str = Files.walk(root))
		{
			Iterator<Path> itr = str.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".class")).iterator();
			while(itr.hasNext())
			{
				var pth = itr.next();
				try(InputStream in = Files.newInputStream(pth))
				{
					visitor.visit(root.relativize(pth).toString().replace(File.separatorChar, '/'), in);
				}
			}
		}
	}
	
	private static <X extends Exception> void visitZipFile(File jarFile, IClassFileVisitor<X> visitor)
			throws IOException, X
	{
		try(ZipFile zf = new ZipFile(jarFile))
		{
			var entries = zf.entries();
			while(entries.hasMoreElements())
			{
				var entry = entries.nextElement();
				if(!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".class"))
				{
					try(InputStream in = zf.getInputStream(entry))
					{
						visitor.visit(entry.getName(), in);
					}
				}
			}
		}
	}
}