package dev.zeith.tsgen.api;

import dev.zeith.tsgen.parse.model.ClassModel;
import dev.zeith.tsgen.parse.src.model.SourceClassModel;
import org.jetbrains.annotations.*;

import java.util.*;

public interface IGenerationExtension
{
	String getId();
	
	boolean defaultEnabled();
	
	@Nullable
	ITypeExtension createForType(@NotNull ClassModel model, @Nullable SourceClassModel srcModel);
	
	static List<IGenerationExtension> get()
	{
		return Storage.EXTENSIONS;
	}
	
	class Storage
	{
		private static final List<IGenerationExtension> EXTENSIONS;
		
		static
		{
			List<IGenerationExtension> ext = new ArrayList<>();
			for(IGenerationExtension e : ServiceLoader.load(IGenerationExtension.class))
				ext.add(e);
			EXTENSIONS = List.copyOf(ext);
		}
	}
}