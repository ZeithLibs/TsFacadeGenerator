package dev.zeith.tsgen.parse.src.parse;

import com.github.javaparser.ParserConfiguration;
import org.jetbrains.annotations.NotNull;

public interface ISourceParserFactory
{
	ISourceParserFactory JAVA_17_PREVIEW = () -> SourceParser.builder().languageLevel(ParserConfiguration.LanguageLevel.JAVA_17_PREVIEW).build();
	ISourceParserFactory JAVA_17 = () -> SourceParser.builder().languageLevel(ParserConfiguration.LanguageLevel.JAVA_17).build();
	ISourceParserFactory JAVA_21 = () -> SourceParser.builder().languageLevel(ParserConfiguration.LanguageLevel.JAVA_21).build();
	ISourceParserFactory JAVA_25 = () -> SourceParser.builder().languageLevel(ParserConfiguration.LanguageLevel.JAVA_25).build();
	ISourceParserFactory BLEEDING_EDGE = () -> SourceParser.builder().languageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE).build();
	ISourceParserFactory RAW = () -> SourceParser.builder().languageLevel(ParserConfiguration.LanguageLevel.RAW).build();
	
	@NotNull
	SourceParser createParser();
}