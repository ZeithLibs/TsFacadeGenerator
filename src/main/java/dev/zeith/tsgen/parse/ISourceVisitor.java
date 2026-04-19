package dev.zeith.tsgen.parse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.zip.*;

interface ISourceVisitor
		extends AutoCloseable
{
	String readSource(String fullPath)
			throws IOException;
	
	default Optional<String> tryReadSource(String fullPath)
	{
		try
		{
			return Optional.ofNullable(readSource(fullPath));
		} catch(Exception ignored) {}
		return Optional.empty();
	}
	
	@Override
	void close()
			throws IOException;
	
	static ISourceVisitor open(File path)
			throws IOException
	{
		if(path == null) return null;
		return path.isDirectory()
			   ? new DirVisitor(path.toPath())
			   : new ZipVisitor(new ZipFile(path));
	}
	
	record ZipVisitor(ZipFile file)
			implements ISourceVisitor
	{
		@Override
		public String readSource(String fullPath)
				throws IOException
		{
			ZipEntry entry = file.getEntry(fullPath);
			if(entry != null && !entry.isDirectory()) try(var in = file.getInputStream(entry))
			{
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			throw new FileNotFoundException(fullPath);
		}
		
		@Override
		public void close()
				throws IOException
		{
			file.close();
		}
	}
	
	record DirVisitor(Path path)
			implements ISourceVisitor
	{
		@Override
		public String readSource(String fullPath)
				throws IOException
		{
			return Files.readString(path.resolve(fullPath));
		}
		
		@Override
		public void close()
		{
		}
	}
}
