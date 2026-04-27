package dev.zeith.tsgen.api;

import dev.zeith.tsgen.parse.model.*;
import dev.zeith.tsgen.parse.src.model.SourceClassModel;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface ITypeExtension
{
	default Stream<ConstructorModel> getExtraConstructors()
	{
		return Stream.empty();
	}
	
	default Stream<FieldModel> getExtraFields()
	{
		return Stream.empty();
	}
	
	default Stream<MethodModel> getExtraMethods()
	{
		return Stream.empty();
	}
	
	static Stream<ConstructorModel> getExtraConstructors(List<ITypeExtension> extensions)
	{
		return extensions.stream().flatMap(ITypeExtension::getExtraConstructors);
	}
	
	static Stream<FieldModel> getExtraFields(List<ITypeExtension> extensions)
	{
		return extensions.stream().flatMap(ITypeExtension::getExtraFields);
	}
	
	static Stream<MethodModel> getExtraMethods(List<ITypeExtension> extensions)
	{
		return extensions.stream().flatMap(ITypeExtension::getExtraMethods);
	}
	
	static List<ITypeExtension> gather(Predicate<IGenerationExtension> enabledExtensions, ClassModel model, SourceClassModel sourceModel)
	{
		return IGenerationExtension.get().stream().filter(enabledExtensions).map(e -> e.createForType(model, sourceModel)).filter(Objects::nonNull).toList();
	}
}