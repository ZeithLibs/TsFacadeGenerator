package dev.zeith.tsgen.api;

import dev.zeith.tsgen.parse.model.*;

import java.util.List;
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
}