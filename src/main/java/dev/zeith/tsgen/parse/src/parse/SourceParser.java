package dev.zeith.tsgen.parse.src.parse;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import lombok.Builder;

public class SourceParser
{
	protected JavaParserAdapter adapter;
	
	@Builder
	public SourceParser(
			ParserConfiguration.LanguageLevel languageLevel
	)
	{
		ParserConfiguration cfg = new ParserConfiguration();
//		if(classPath != null) cfg.setSymbolResolver(new JavaSymbolSolver(classPath.typeSolver.build()));
		cfg.setLanguageLevel(languageLevel);
		this.adapter = new JavaParserAdapter(new JavaParser(cfg));
	}
	
	public CompilationUnit parse(String code)
	{
		return adapter.parse(code);
	}
}