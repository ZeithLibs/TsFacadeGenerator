package dev.zeith.tsgen.parse.sig;

import dev.zeith.tsgen.parse.NullAwareType;
import dev.zeith.tsgen.util.TypeUtil;
import lombok.*;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public record SimpleGeneric(Type base, String paramRef, @With int dimensions, List<SimpleGeneric> typeArgs, WildcardKind wildcard)
{
	public static SimpleGeneric ofStrictType(Type type)
	{
		return ofStrictTypeWithTypeArgs(type, List.of());
	}
	
	public static SimpleGeneric ofStrictTypeWithTypeArgs(Type type, List<SimpleGeneric> typeArgs)
	{
		return new SimpleGeneric(
				type,
				null,
				0,
				typeArgs,
				WildcardKind.NONE
		);
	}
	
	public Set<Type> getImports()
	{
		Set<Type> list = new HashSet<>();
		visitTypes(t ->
		{
			var f = NullAwareType.findImport(t);
			if(f != null) list.add(f);
		});
		return list;
	}
	
	public boolean isTypeVariable()
	{
		return paramRef != null;
	}
	
	public void visitTypes(Consumer<Type> visitor)
	{
		if(base != null)
			visitor.accept(base);
		
		if(typeArgs != null)
			typeArgs.forEach(s -> s.visitTypes(visitor));
	}
	
	public boolean isArray()
	{
		return dimensions > 0;
	}
	
	public String getTsSimpleName()
	{
//		if(wildcard == WildcardKind.SUPER)
//			return "unknown"; // safest fallback
		
		if(wildcard == WildcardKind.ANY)
			return "any";
		
		String val;
		if(paramRef != null) val = paramRef;
		else val = TypeUtil.typeToTs(base);
		var core = val + (isParameterized() ? parameters() : "");
		
		if(wildcard == WildcardKind.EXTENDS)
			return core; // TS has no extends wildcard
		
		return core + (isArray() ? "[]".repeat(dimensions) : "");
	}
	
	public String parameters()
	{
		return "<" + typeArgs.stream().map(SimpleGeneric::getTsSimpleName).collect(Collectors.joining(", ")) + ">";
	}
	
	public boolean isParameterized()
	{
		return !typeArgs.isEmpty();
	}
	
	public static MethodGeneric parseMethodDescSignature(String signature)
	{
		if(signature == null || signature.isEmpty())
			return null;
		
		Parser p = new Parser(signature);
		
		// ---------------- TYPE PARAMETERS (MUST BE FIRST) ----------------
		List<TypeParameter> typeParams = null;
		Map<String, TypeParameter> typeParamMap = new HashMap<>();
		
		if(p.peek() == '<')
		{
			p.read(); // '<'
			typeParams = new ArrayList<>();
			
			while(p.peek() != '>')
			{
				TypeParameter tp = p.parseTypeParameter();
				typeParams.add(tp);
				typeParamMap.put(tp.name(), tp);
			}
			
			p.read(); // '>'
		}
		
		// ---------------- METHOD PARAMS ----------------
		if(p.peek() != '(')
			throw new IllegalArgumentException("Expected '(' in signature: " + signature);
		
		p.read(); // '('
		
		List<SimpleGeneric> params = new ArrayList<>();
		while(p.peek() != ')')
		{
			params.add(p.parseType(typeParamMap));
		}
		
		p.read(); // ')'
		
		// ---------------- RETURN TYPE ----------------
		SimpleGeneric returnType = p.parseType(typeParamMap);
		
		return new MethodGeneric(typeParams, params, returnType);
	}
	
	@SneakyThrows
	public static SimpleGeneric parse(String signature)
	{
		if(signature == null) return null;
		Parser p = new Parser(signature);
		return p.parseType(null);
	}
	
	public static final class Parser
	{
		private final String sig;
		private int i;
		
		Parser(String sig)
		{
			this.sig = sig;
		}
		
		boolean eof()
		{
			return i >= sig.length();
		}
		
		char peek()
		{
			return sig.charAt(i);
		}
		
		char read()
		{
			return sig.charAt(i++);
		}
		
		boolean consume(char c)
		{
			if(!eof() && peek() == c)
			{
				i++;
				return true;
			}
			return false;
		}
		
		SimpleGeneric parseType(Map<String, TypeParameter> typeParams)
		{
			// ---------------- ARRAY PREFIX ----------------
			int dims = 0;
			while(!eof() && peek() == '[')
			{
				read();
				dims++;
			}
			
			// not sure what this is, but it comes as +Ljava/type/...
			WildcardKind wildcard = WildcardKind.NONE;
			
			if(!eof())
			{
				char c = peek();
				if(c == '+')
				{
					wildcard = WildcardKind.EXTENDS;
					read();
				} else if(c == '-')
				{
					wildcard = WildcardKind.SUPER;
					read();
				} else if(c == '*')
				{
					read();
					return new SimpleGeneric(
							null,
							null,
							dims,
							List.of(),
							WildcardKind.ANY
					);
				}
			}
			
			// ---------------- PRIMITIVE ----------------
			if(!eof())
			{
				char c = peek();
				
				if(c != 'L' && c != 'T')
				{
					// primitive
					char prim = read();
					Type base;
					
					try
					{
						base = Type.getType(String.valueOf(prim));
					} catch(IllegalArgumentException e)
					{
						throw new IllegalStateException("Failed to parse: signature < " + sig + " > @ " + i, e);
					}
					
					return new SimpleGeneric(
							base,
							null,
							dims,
							List.of(),
							wildcard
					);
				}
			}
			
			// ---------------- TYPE VARIABLE ----------------
			if(!eof())
			{
				char c = peek();
				
				if(c == 'T')
				{
					read(); // 'T'
					
					StringBuilder name = new StringBuilder();
					
					while(!eof())
					{
						char ch = read();
						if(ch == ';') break;
						name.append(ch);
					}
					
					String tName = name.toString();
					
					if(typeParams != null && typeParams.containsKey(tName))
					{
						return new SimpleGeneric(
								null,
								tName,
								dims,
								List.of(),
								wildcard
						);
					}
					
					return new SimpleGeneric(
							null,
							tName,
							dims,
							List.of(),
							wildcard
					);
				}
			}
			
			// ---------------- OBJECT TYPE ----------------
			if(read() != 'L')
				throw new IllegalStateException("Expected L");
			
			StringBuilder internal = new StringBuilder();
			
			while(!eof())
			{
				char c = read();
				
				if(c == '<')
				{
					Type base = Type.getObjectType(internal.toString());
					
					List<SimpleGeneric> args = new ArrayList<>();
					
					while(peek() != '>')
					{
						args.add(parseType(null));
					}
					
					read(); // '>'
					
					if(!eof() && peek() == ';')
						read();
					
					return new SimpleGeneric(
							base,
							null,
							dims,
							args,
							wildcard
					);
				}
				
				if(c == ';')
					break;
				
				internal.append(c);
			}
			
			Type base = Type.getObjectType(internal.toString());
			
			return new SimpleGeneric(
					base,
					null,
					dims,
					List.of(),
					wildcard
			);
		}
		
		TypeParameter parseTypeParameter()
		{
			// name
			StringBuilder name = new StringBuilder();
			while(peek() != ':')
				name.append(read());
			
			read(); // ':'
			
			List<SimpleGeneric> classBounds = new ArrayList<>();
			List<SimpleGeneric> interfaceBounds = new ArrayList<>();
			
			// first bound is class bound (optional but if present, it's class)
			if(peek() != ':')
				classBounds.add(parseType(null));
			
			// interface bounds
			while(peek() == ':')
			{
				read();
				interfaceBounds.add(parseType(null));
			}
			
			return new TypeParameter(name.toString(), classBounds, interfaceBounds);
		}
	}
}