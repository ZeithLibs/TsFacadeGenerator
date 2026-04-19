import dev.zeith.tsgen.*;
import dev.zeith.tsgen.parse.IClassFileVisitor;
import dev.zeith.tsgen.parse.model.ClassModel;
import dev.zeith.tsgen.parse.src.model.SourceClassModel;
import dev.zeith.tsgen.parse.src.parse.ISourceParserFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TestTsGen
{
	public static void main(String[] args)
			throws IOException
	{
		File targetDir = new File("build/generated/typescript");
		
		System.out.println("Generating to " + targetDir.getAbsoluteFile());
		
		// Clean directory before dumping ts
		deleteDir(targetDir);
		
		File input = new File("build/classes/java/main");
		
		BulkTypeScriptExporter exporter = BulkTypeScriptExporter
				.builder()
				.outDir(targetDir)
				// if you want to separate into individual classes:
				.pathResolver(IPathResolver.FROM_CLASS_NAME)
				.build();
		
		var srcParser = ISourceParserFactory.BLEEDING_EDGE.createParser();
		
		// Check every file/entry in dir/jar
		IClassFileVisitor.visit(input, Path.of("src/main/java").toFile(), (name, entry, src) ->
				{
					byte[] bytecode = entry.readAllBytes();
					ClassModel model = ClassModel.parse(bytecode);
					if(model == null || !model.name().getInternalName().contains("/") || !model.isPublic())
					{
						System.out.println("Warn: Class model skipped for " + name);
						return;
					}
					
					Map<String, SourceClassModel> classes = src.map(code -> SourceClassModel.parse(srcParser, code)).orElse(Map.of());
					
					exporter.export(model, classes.get(model.getSimpleName()));
				}
		);
		
		// Optimize imports in the generated files
		exporter.optimize();
		
		// if you want to perform more than one export,
		// this prevents optimize() calls from fixing imports on files that were already exported (unless the files will be exported into again)
		exporter.reset();
	}
	
	public static void deleteDir(File dir)
			throws IOException
	{
		if(!dir.exists()) return;
		try(var str = Files.walk(dir.toPath()))
		{
			var itr = str.sorted(Comparator.reverseOrder()).iterator();
			while(itr.hasNext())
			{
				Files.delete(itr.next());
			}
		}
	}
}