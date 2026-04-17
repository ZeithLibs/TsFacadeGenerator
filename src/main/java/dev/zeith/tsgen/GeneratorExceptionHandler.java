package dev.zeith.tsgen;

public enum GeneratorExceptionHandler
{
	GLOBAL_FAIL,
	SKIP_FAILED_ENTRY;
	
	public boolean shouldSkip()
	{
		return this == SKIP_FAILED_ENTRY;
	}
}