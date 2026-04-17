package dev.zeith.tsgen.imports;

import dev.zeith.tsgen.util.TypeUtil;
import lombok.Setter;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

public abstract class BaseImportModel
		implements IImportModel, Cloneable
{
	@Setter
	protected Function<Type, String> filePath = ofType -> ofType.getInternalName() + ".ts";
	
	protected abstract BaseImportModel cloneInstance();
	
	protected abstract String createImport(Stream<String> importedObjects, String importPath);
	
	protected String resolvePath(Type ourType, Type importType)
	{
		return TypeUtil.relativePath(
				filePath.apply(ourType),
				filePath.apply(importType)
		);
	}
	
	protected String getTypeName(Type importType)
	{
		return TypeUtil.getSimpleName(importType);
	}
	
	@Override
	public String generateImports(String newline, Type ourType, Iterable<Type> importType)
	{
		Map<String, List<Type>> fromSamePath = new HashMap<>();
		for(Type type : importType)
			fromSamePath
					.computeIfAbsent(resolvePath(ourType, type), k -> new ArrayList<>())
					.add(type);
		return fromSamePath
				.entrySet()
				.stream()
				.map(e -> createImport(e.getValue().stream().map(this::getTypeName), e.getKey()))
				.collect(Collectors.joining(newline));
	}
	
	@Override
	public String reduceImports(String filename, String newline, String importLines)
	{
		var grouped = parseImports(importLines);
		grouped.remove("./" + filename);
		StringBuilder out = new StringBuilder();
		for(var entry : grouped.entrySet())
			out.append(createImport(entry.getValue().stream(), entry.getKey())).append(newline);
		return out.toString().trim();
	}
	
	@Override
	public final BaseImportModel clone()
	{
		var inst = cloneInstance();
		inst.filePath = this.filePath;
		return inst;
	}
}