package dev.zeith.tsgen.parse.src.parse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.Type;
import dev.zeith.tsgen.parse.src.type.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ParseContext
{
	protected final Map<String, String> classImports = new HashMap<>();
	protected final Set<String> packageImports = new HashSet<>(
			Set.of("java/lang")
	);
	
	public final Set<String> importedPackages = Collections.unmodifiableSet(packageImports);
	
	public ParseContext(String ourPackage)
	{
		if(ourPackage != null)
			packageImports.add(ourPackage.replace('.', '/'));
	}
	
	public ISourceType parseType(Type type)
	{
		String name;
		
		if(type.isPrimitiveType() || type.isVoidType())
			return new ResolvedType(type.asString(), type.toDescriptor(), null, this);
		else
			name = type.asString();
		
		int genericStart = name.indexOf('<');
		int genericEnd = name.indexOf('>');
		
		String generic = null;
		if(genericStart != -1 && genericEnd != -1)
		{
			generic = name.substring(genericStart + 1, genericEnd);
			name = name.substring(0, genericStart);
		}
		
		if(name.contains("."))
		{
			// Likely a resolved returnType that doesn't use imports
			new ResolvedType(getSimpleClassName(name), name.replace('.', '/'), generic, this);
		}
		
		var fullClNm = classImports.get(name);
		if(fullClNm != null) return new ResolvedType(name, fullClNm.replace('.', '/'), generic, this);
		
		return new UndecidedType(name, generic, this);
	}
	
	public @Nullable String parseComment(Node node)
	{
		var rawComment = node.getComment().orElse(null);
		String comment = null;
		if(rawComment != null && (rawComment.isLineComment() || rawComment.isJavadocComment() || rawComment.isBlockComment()))
			comment = rawComment.asString();
		return comment != null ? comment.replace("\r", "") : null;
	}
	
	public void registerWildcardImport(String packageWildcard)
	{
		packageImports.add(packageWildcard.replace('.', '/'));
	}
	
	public void registerClassImport(String className)
	{
		classImports.put(getSimpleClassName(className), className);
	}
	
	public static String getSimpleClassName(String className)
	{
		return className.substring(className.lastIndexOf('.') + 1);
	}
}