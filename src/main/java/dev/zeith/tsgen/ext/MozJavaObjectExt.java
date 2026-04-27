package dev.zeith.tsgen.ext;

import dev.zeith.tsgen.api.*;
import dev.zeith.tsgen.parse.*;
import dev.zeith.tsgen.parse.model.*;
import dev.zeith.tsgen.parse.sig.SimpleGeneric;
import dev.zeith.tsgen.parse.src.model.SourceClassModel;
import org.jetbrains.annotations.*;

import java.util.List;
import java.util.stream.Stream;

public class MozJavaObjectExt
		implements IGenerationExtension
{
	@Override
	public String getId()
	{
		return "org.mozilla:rhino:__javaObject__";
	}
	
	@Override
	public boolean defaultEnabled()
	{
		return false;
	}
	
	@Override
	public @Nullable ITypeExtension createForType(@NotNull ClassModel model, @Nullable SourceClassModel srcModel)
	{
		var field = new FieldModel(true, "__javaObject__",
				new NullAwareType(-1, null, EnumNullability.NOT_NULL, NullAwareType.JAVA_LANG_CLASS,
						SimpleGeneric.ofStrictTypeWithTypeArgs(
								NullAwareType.JAVA_LANG_CLASS,
								List.of(SimpleGeneric.ofStrictType(model.name()))
						)
				),
				null
		);
		return new MozJavaObjectTypeExt(field);
	}
	
	record MozJavaObjectTypeExt(FieldModel field)
			implements ITypeExtension
	{
		@Override
		public Stream<FieldModel> getExtraFields()
		{
			return Stream.of(field);
		}
	}
}
