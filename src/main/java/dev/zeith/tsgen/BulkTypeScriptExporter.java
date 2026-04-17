package dev.zeith.tsgen;

import dev.zeith.tsgen.imports.*;
import dev.zeith.tsgen.parse.ClassModel;
import dev.zeith.tsgen.util.TypeUtil;
import lombok.Builder;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

public class BulkTypeScriptExporter
{
	protected final File outDir;
	protected final BaseImportModel importModel;
	protected final Consumer<TypeScriptGenerator> configurator;
	protected final Function<Type, String> filePath;
	
	protected final Set<File> toOptimize = ConcurrentHashMap.newKeySet();
	
	@Builder
	public BulkTypeScriptExporter(
			File outDir,
			BaseImportModel importModel,
			Consumer<TypeScriptGenerator> configurator,
			Function<Type, String> filePath
	)
	{
		this.outDir = Objects.requireNonNull(outDir, "outDir");
		this.importModel = (importModel != null ? importModel : FromImportModel.INSTANCE).clone();
		this.importModel.setFilePath(this::getFilePathOf);
		this.configurator = configurator != null ? configurator : gen ->
		{
		};
		this.filePath = filePath != null ? filePath : pathFromPackage();
	}
	
	public void export(ClassModel model)
			throws IOException
	{
		File dst = new File(outDir, getFilePathOf(model.name())).getAbsoluteFile();
		toOptimize.add(dst);
		
		// Create parent directory
		dst.toPath().getParent().toFile().mkdirs();
		
		TypeScriptGenerator gen = new TypeScriptGenerator(model);
		StringBuilder sb = new StringBuilder();
		
		configurator.accept(gen);
		
		if(dst.isFile())
		{
			gen.generate(sb, false);
			String ts = sb.toString();
			
			String str = gen.generateImports()
					+ "\n"
					+ Files.readString(dst.toPath(), StandardCharsets.UTF_8)
					+ "\n"
					+ ts;
			
			Files.writeString(dst.toPath(), str, StandardCharsets.UTF_8);
		} else
		{
			gen.generate(sb, true);
			try(FileOutputStream out = new FileOutputStream(dst))
			{
				out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
			}
		}
	}
	
	public void optimize()
			throws IOException
	{
		for(File file : toOptimize)
		{
			String optimized = importModel.reduceImports(file.getName(), Files.lines(file.toPath()));
			if(optimized != null) Files.writeString(file.toPath(), optimized, StandardCharsets.UTF_8);
		}
	}
	
	public void reset()
	{
		toOptimize.clear();
	}
	
	public String getFilePathOf(Type name)
	{
		return filePath.apply(name);
	}
	
	public static Function<Type, String> pathFromPackage()
	{
		return type -> TypeUtil.getPackagePath(type) + ".ts";
	}
	
	public static Function<Type, String> pathFromClass()
	{
		return type -> type.getInternalName() + ".ts";
	}
}