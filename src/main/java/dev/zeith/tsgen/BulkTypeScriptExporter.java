package dev.zeith.tsgen;

import dev.zeith.tsgen.imports.*;
import dev.zeith.tsgen.parse.model.ClassModel;
import dev.zeith.tsgen.parse.src.model.SourceClassModel;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BulkTypeScriptExporter
{
	protected final File outDir;
	protected final BaseImportModel importModel;
	protected final Consumer<TypeScriptGenerator> configurator;
	protected final @Getter IPathResolver pathResolver;
	
	@Getter
	protected final Set<File> toOptimize = ConcurrentHashMap.newKeySet();
	
	@Builder
	public BulkTypeScriptExporter(
			File outDir,
			BaseImportModel importModel,
			Consumer<TypeScriptGenerator> configurator,
			IPathResolver pathResolver
	)
	{
		this.outDir = Objects.requireNonNull(outDir, "outDir");
		this.importModel = (importModel != null ? importModel : FromImportModel.INSTANCE).clone();
		this.importModel.setFilePath(this::getFilePathOf);
		this.configurator = configurator != null ? configurator : gen ->
		{
		};
		this.pathResolver = pathResolver != null ? pathResolver : IPathResolver.FROM_PACKAGE;
	}
	
	public File export(ClassModel model)
			throws IOException
	{
		return export(model, null);
	}
	
	public File export(ClassModel model, @Nullable SourceClassModel sourceModel)
			throws IOException
	{
		File dst = new File(outDir, getFilePathOf(model.name())).getAbsoluteFile();
		toOptimize.add(dst);
		
		// Create parent directory
		dst.toPath().getParent().toFile().mkdirs();
		
		TypeScriptGenerator gen = new TypeScriptGenerator(model, sourceModel).withImportModel(this.importModel);
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
		
		return dst;
	}
	
	public void optimize()
			throws IOException
	{
		for(File file : toOptimize)
			optimize(file, importModel);
	}
	
	public void optimize(String prefix, String suffix)
			throws IOException
	{
		for(File file : toOptimize)
			optimize(file, importModel, prefix, suffix);
	}
	
	public static void optimize(File file, IImportModel importModel)
			throws IOException
	{
		optimize(file, importModel, "", "");
	}
	
	public static void optimize(File file, IImportModel importModel, String prefix, String suffix)
			throws IOException
	{
		String optimized = importModel.reduceImports(file.getName(), Files.lines(file.toPath()));
		Files.writeString(file.toPath(), prefix + optimized + suffix, StandardCharsets.UTF_8);
	}
	
	public void reset()
	{
		toOptimize.clear();
	}
	
	public String getFilePathOf(String internalName)
	{
		return pathResolver.getPath(Type.getObjectType(internalName));
	}
	
	public String getFilePathOf(Type name)
	{
		return pathResolver.getPath(name);
	}
}