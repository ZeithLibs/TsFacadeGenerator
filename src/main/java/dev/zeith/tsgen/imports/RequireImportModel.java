package dev.zeith.tsgen.imports;

import dev.zeith.tsgen.util.StringQuoter;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.*;

public class RequireImportModel
		extends BaseImportModel
{
	public static final RequireImportModel INSTANCE = new RequireImportModel();
	
	public static final Pattern REGEX = Pattern.compile("const\\s*\\{(?<objects>\\s*[^}]+)}\\s*=\\s*require\\s*\\(\\s*['\"](?<path>.+)['\"]\\s*\\)\\s*;?");
	public static final Predicate<String> FILTER = REGEX.asMatchPredicate();
	
	@Override
	public BaseImportModel cloneInstance()
	{
		return new RequireImportModel();
	}
	
	@Override
	protected String createImport(Stream<String> importedObjects, String importPath)
	{
		return "const { " + importedObjects.collect(Collectors.joining(", ")) + " } = require(" + StringQuoter.quote(importPath) + ");";
	}
	
	@Override
	public Map<String, Set<String>> parseImports(String importLines)
	{
		Map<String, Set<String>> grouped = new LinkedHashMap<>();
		
		for(String line : importLines.split("\n"))
		{
			line = line.trim();
			if(!line.startsWith("const")) continue;
			
			int start = line.indexOf('{');
			int end = line.indexOf('}');
			int reqStart = line.indexOf("require(");
			
			if(start < 0 || end < 0 || reqStart < 0) continue;
			
			String symbols = line.substring(start + 1, end).trim();
			String path = line.substring(reqStart + 8, line.lastIndexOf(')'));
			path = path.replace("\"", "").replace("'", "");
			
			grouped.computeIfAbsent(path, k -> new LinkedHashSet<>());
			
			for(String s : symbols.split(","))
				grouped.get(path).add(s.trim());
		}
		
		return grouped;
	}
	
	@Override
	public boolean isImport(String line)
	{
		return FILTER.test(line);
	}
}