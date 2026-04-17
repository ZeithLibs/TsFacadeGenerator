package dev.zeith.tsgen.parse.sig;

import java.util.*;

public record ClassGeneric(
		List<TypeParameter> typeParameters,
		SimpleGeneric superClass,
		List<SimpleGeneric> interfaces
)
{
	public static ClassGeneric parseClassSignature(String signature)
	{
		if(signature == null || signature.isEmpty())
			return null;
		
		var p = new SimpleGeneric.Parser(signature);
		
		List<TypeParameter> typeParams = List.of();
		
		// ---------------- TYPE PARAMETERS ----------------
		if(p.peek() == '<')
		{
			p.read(); // '<'
			typeParams = new ArrayList<>();
			
			while(p.peek() != '>')
			{
				typeParams.add(p.parseTypeParameter());
			}
			
			p.read(); // '>'
		}
		
		// ---------------- SUPER CLASS ----------------
		SimpleGeneric superClass = p.parseType(null);
		
		// ---------------- INTERFACES ----------------
		List<SimpleGeneric> interfaces = new ArrayList<>();
		while(!p.eof())
		{
			interfaces.add(p.parseType(null));
		}
		
		return new ClassGeneric(typeParams, superClass, interfaces);
	}
}
