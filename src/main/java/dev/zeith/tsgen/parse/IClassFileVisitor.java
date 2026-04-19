package dev.zeith.tsgen.parse;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

public interface IClassFileVisitor<X extends Exception>
{
	void visit(String path, InputStream input, Optional<String> sourceCode)
			throws IOException, X;
	
	static <X extends Exception> void visit(File jarOrDir, File sourceJarOrDir, IClassFileVisitor<X> visitor)
			throws IOException, X
	{
		if(!jarOrDir.exists()) return;
		if(jarOrDir.isDirectory()) visitDir(jarOrDir, sourceJarOrDir, visitor);
		else if(jarOrDir.isFile())
		{
			String name = jarOrDir.getName().toLowerCase(Locale.ROOT);
			if(name.endsWith(".jar") || name.endsWith(".zip"))
				visitZipFile(jarOrDir, sourceJarOrDir, visitor);
		}
	}
	
	private static <X extends Exception> void visitDir(File dir, File sourceJarOrDir, IClassFileVisitor<X> visitor)
			throws IOException, X
	{
		var root = dir.toPath();
		try(var str = Files.walk(root); var src = ISourceVisitor.open(sourceJarOrDir))
		{
			Iterator<Path> itr = str.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".class")).iterator();
			while(itr.hasNext())
			{
				var pth = itr.next();
				try(InputStream in = Files.newInputStream(pth))
				{
					String name = root.relativize(pth).toString().replace(File.separatorChar, '/');
					
					String sourceFilePath = name.substring(0, name.length() - 6);
					int filename = sourceFilePath.lastIndexOf('/') + 1;
					int subclass = sourceFilePath.indexOf('$', filename);
					if(subclass != -1)
						sourceFilePath = sourceFilePath.substring(0, subclass);
					
					visitor.visit(name, in, src.tryReadSource(sourceFilePath + ".java"));
				}
			}
		}
	}
	
	private static <X extends Exception> void visitZipFile(File jarFile, File sourceJarOrDir, IClassFileVisitor<X> visitor)
			throws IOException, X
	{
		try(ZipFile zf = new ZipFile(jarFile); var src = ISourceVisitor.open(sourceJarOrDir))
		{
			var entries = zf.entries();
			while(entries.hasMoreElements())
			{
				var entry = entries.nextElement();
				if(!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".class"))
				{
					try(InputStream in = zf.getInputStream(entry))
					{
						String name = entry.getName();
						
						String sourceFilePath = name.substring(0, name.length() - 6);
						int filename = sourceFilePath.lastIndexOf('/') + 1;
						int subclass = sourceFilePath.indexOf('$', filename);
						if(subclass != -1)
							sourceFilePath = sourceFilePath.substring(0, subclass);
						
						visitor.visit(name, in, src.tryReadSource(sourceFilePath + ".java"));
					}
				}
			}
		}
	}
	
}