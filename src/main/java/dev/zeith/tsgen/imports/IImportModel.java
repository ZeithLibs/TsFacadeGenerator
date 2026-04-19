package dev.zeith.tsgen.imports;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Stream;

public interface IImportModel
{
	Comparator<Type> IMPORT_COMPARATOR = (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getInternalName(), o2.getInternalName());
	
	String generateImports(String newline, Type ourType, Iterable<Type> importType);
	
	Map<String, Set<String>> parseImports(String importLines);
	
	String reduceImports(String filename, String newline, String importLines);
	
	boolean isImport(String line);
	
	@NotNull
	default String reduceImports(String filename, Stream<String> in)
	{
		String newline = "\n";
		StringBuilder imports = new StringBuilder();
		StringBuilder types = new StringBuilder();
		boolean isImports = true;
		
		try(in)
		{
			var itr = in.iterator();
			while(itr.hasNext())
			{
				String ln = itr.next();
				
				importHandler:
				if(isImports)
				{
					imports.append(ln).append(newline);
					if(!ln.isBlank() && !isImport(ln))
					{
						isImports = false;
						break importHandler;
					}
					continue;
				}
				
				types.append(ln).append(newline);
			}
		}
		
		String oldImps = imports.toString();
		String imps = reduceImports(filename, newline, oldImps) + newline;
		
		return imps + newline + types;
	}
}