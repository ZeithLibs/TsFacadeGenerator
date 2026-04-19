package dev.zeith.tsgen.imports;

import dev.zeith.tsgen.util.StringQuoter;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.*;

public class FromImportModel
		extends BaseImportModel
{
	public static final FromImportModel INSTANCE = new FromImportModel();
	
	public static final Pattern REGEX = Pattern.compile("import\\s*\\{(?<objects>\\s*[^}]+)}\\s*from\\s*['\"](?<path>.+)['\"];");
	public static final Predicate<String> FILTER = REGEX.asMatchPredicate();
	
	@Override
	public BaseImportModel cloneInstance()
	{
		return new FromImportModel();
	}
	
	@Override
	protected String createImport(Stream<String> importedObjects, String importPath)
	{
		return "import { " + importedObjects.collect(Collectors.joining(", ")) + " } from " + StringQuoter.quote(importPath) + ";";
	}
	
	@Override
	public boolean isImport(String line)
	{
		return FILTER.test(line);
	}
	
	@Override
	public Map<String, Set<String>> parseImports(String importLines)
	{
		Map<String, Set<String>> grouped = new LinkedHashMap<>();
		
		for(String line : importLines.split("\n"))
		{
			line = line.trim();
			if(!line.startsWith("import")) continue;
			
			int start = line.indexOf('{');
			int end = line.indexOf('}');
			int fromIdx = line.indexOf("from");
			
			if(start < 0 || end < 0 || fromIdx < 0) continue;
			
			String symbols = line.substring(start + 1, end).trim();
			String path = line.substring(fromIdx + 4).replace(";", "").trim();
			path = path.replace("\"", "").replace("'", "");
			
			grouped.computeIfAbsent(path, k -> new LinkedHashSet<>());
			
			for(String s : symbols.split(","))
				grouped.get(path).add(s.trim());
		}
		
		return grouped;
	}
}