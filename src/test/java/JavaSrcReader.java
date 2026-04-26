import dev.zeith.tsgen.*;
import dev.zeith.tsgen.parse.model.ClassModel;
import dev.zeith.tsgen.parse.src.model.SourceClassModel;
import dev.zeith.tsgen.parse.src.parse.ISourceParserFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class JavaSrcReader
{
	public static void main(String[] args)
			throws IOException
	{
		File targetDir = new File("build/generated/typescript");
		
		System.out.println("Generating to " + targetDir.getAbsoluteFile());
		
		// Clean directory before dumping ts
		deleteDir(targetDir);
		
		Path root = Path.of("P:\\$Code\\Minecraft\\Commissions\\Permanent\\Storytelling-8.5");
		String binFolder = "build/classes/java/main/";
		String srcFolder = "src/main/java/";
		
		List<String> allExports = List.of(
//				"dev/zeith/tsgen/TypeScriptGenerator",
//				"dev/zeith/tsgen/IPathResolver",
//				"dev/zeith/tsgen/GeneratorExceptionHandler",
//				"dev/zeith/tsgen/BulkTypeScriptExporter"
				"com/storyteam/storytelling/script/performer/builder/PerformerBuilder",
				"com/storyteam/storytelling/script/performer/builder/ScriptPerformerBuilder"
		);
		
		BulkTypeScriptExporter exporter = BulkTypeScriptExporter
				.builder()
				.outDir(targetDir)
				.pathResolver(IPathResolver.FROM_PACKAGE)
				.build();
		
		for(String classPath : allExports)
		{
			String code = Files.readString(root.resolve(srcFolder + classPath + ".java"));
			byte[] bytecode = Files.readAllBytes(root.resolve(binFolder + classPath + ".class"));
			
			var srcParser = ISourceParserFactory.BLEEDING_EDGE.createParser();
			
			var model = ClassModel.parse(bytecode);
			Map<String, SourceClassModel> classes = Objects.requireNonNullElse(SourceClassModel.parse(srcParser, code), Collections.emptyMap());
			
			exporter.export(model, classes.get(model.getSimpleName()));
		}
		
		exporter.optimize();
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