package dev.zeith.tsgen;

import dev.zeith.tsgen.api.*;
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
import java.util.function.*;

public class BulkTypeScriptExporter
{
	protected final File outDir;
	protected final BaseImportModel importModel;
	protected final Consumer<TSGenSettings.TSGenSettingsBuilder> configurator;
	protected final @Getter IPathResolver pathResolver;
	
	@Getter
	protected final Map<File, String> toOptimize = new ConcurrentHashMap<>();
	
	@Builder
	public BulkTypeScriptExporter(
			File outDir,
			BaseImportModel importModel,
			Consumer<TSGenSettings.TSGenSettingsBuilder> configurator,
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
		return export(model, sourceModel, IGenerationExtension.DEFAULT_ENABLED);
	}
	
	public File export(ClassModel model, @Nullable SourceClassModel sourceModel, Predicate<IGenerationExtension> enabledExtensions)
			throws IOException
	{
		return export(model, sourceModel, ITypeExtension.gather(enabledExtensions, model, sourceModel));
	}
	
	public File export(ClassModel model, @Nullable SourceClassModel sourceModel, List<ITypeExtension> typeExtensions)
			throws IOException
	{
		File dst = new File(outDir, getFilePathOf(model.name())).getAbsoluteFile();
		
		// Create parent directory
		dst.toPath().getParent().toFile().mkdirs();
		
		var settings = TSGenSettings.builder().importModel(this.importModel);
		configurator.accept(settings);
		var set = settings.build();
		toOptimize.put(dst, set.newline);
		TypeScriptGenerator gen = new TypeScriptGenerator(set, model, sourceModel, typeExtensions);
		StringBuilder sb = new StringBuilder();
		
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
		for(var e : toOptimize.entrySet())
			optimize(e.getKey(), importModel, e.getValue());
	}
	
	public void optimize(String prefix, String suffix)
			throws IOException
	{
		for(var e : toOptimize.entrySet())
			optimize(e.getKey(), importModel, e.getValue(), prefix, suffix);
	}
	
	public static void optimize(File file, IImportModel importModel, String newline)
			throws IOException
	{
		optimize(file, importModel, newline, "", "");
	}
	
	public static void optimize(File file, IImportModel importModel, String newline, String prefix, String suffix)
			throws IOException
	{
		String optimized = importModel.reduceImports(newline, file.getName(), Files.lines(file.toPath()));
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