package dev.zeith.tsgen;

import dev.zeith.tsgen.imports.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@With
@Getter
@Builder
public final class TSGenSettings
{
	@Builder.Default
	final boolean enableVarargs = true;
	
	@NotNull
	@Builder.Default
	final String newline = "\n";
	
	@NotNull
	@Builder.Default
	final String indent = "\t";
	
	@NotNull
	@Builder.Default
	final IImportModel importModel = RequireImportModel.INSTANCE;
	
	@NotNull
	@Builder.Default
	final GeneratorExceptionHandler exceptionHandler = GeneratorExceptionHandler.GLOBAL_FAIL;
}