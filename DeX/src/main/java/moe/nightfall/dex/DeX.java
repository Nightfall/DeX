package moe.nightfall.dex;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import moe.nightfall.dex.DeXSerializable.SerializationMap;
import moe.nightfall.dex.DeXSerializable.StaticSerialization;

public final class DeX {
	private DeX() {}
	
	@SuppressWarnings("unchecked")
	public static <T> T coerce(Class<T> target, Object o) {
		if (o == null) return null;
		if (target.isInstance(o)) {
			return (T) o;
		}
		if (o instanceof DeXTable) {
			return StaticSerialization.get(target).deserialize((DeXTable) o);
		}
		throw new IllegalArgumentException(o + " cant be coerced into " + target);
	}
	
	@SuppressWarnings("unchecked")
	// FIXME This in unchecked and I probably can't tell if it fails due to an incorrect
	// cast, might want to add a class parameter to this
	public static <T> T deserialize(DeXTable table, SerializationMap sel) {
		return (T) sel.byTable(table).deserialize(table);
	}
	
	// Used for serialization
	
	public static Object toDeX(Object in) {
		return toDeX(in, SerializationMap.empty);
	}
	
	public static Object toDeX(Object in, SerializationMap sel) {
		if (in == null) return null;
		if (isPrimitive(in)) return in;
		
		if (in instanceof Object[]) return arrayToTable((Object[])in, sel);
		if (in instanceof Iterable) return iterableToTable((Iterable<?>)in, sel);
		if (in instanceof Map) return mapToTable((Map<?, ?>)in, sel);
		
		// Sanity check
		if (in instanceof DeXTable) return in;
		if (in instanceof DeXArray) return ((DeXArray)in).toDeXTable();

		// Raw types because generics are too dump to handle this
		return StaticSerialization.get((Class)in.getClass()).serialize(in, sel);
	}
	
	/** 
	 * This ensures that the given objects is wrapped in a {@link DeXTable}
	 * Don't call this method with anything other than primitives and {@link DeXTable}! 
	 */
	public static DeXTable ensure(Object in) {
		if (in instanceof DeXTable) return (DeXTable) in;
		else {
			return DeXTable.builder("", 1).add(in).create();
		}
	}
	
	public static Object toJava(Class<?> target, Object in) {
		
		if (in == null) return null;
		if (isPrimitive(in)) return in;
		DeXTable table = null;
		if (in instanceof DeXTable) table = (DeXTable) in;
		else if (in instanceof DeXArray) table = ((DeXArray) in).toDeXTable();
		else return in;
		
		// Note that we can only deserialize to some default type. The method above can serialize anything.
		if (target.isAssignableFrom(HashMap.class)) return table.toHashMap();
		
		// We can cope with the most basic collections
		// TODO What about Vector? Or is that thing even used nowadays?
		if (target.isAssignableFrom(ArrayList.class)) return new ArrayList<>(tableToCollection(table));
		if (target.isAssignableFrom(LinkedList.class)) return new LinkedList<>(tableToCollection(table));
		if (target.isAssignableFrom(HashSet.class)) return new HashSet<>(tableToCollection(table));
		
		if (target.isArray()) return tableToCollection(table).toArray();
		
		// Sanity check
		if (target == DeXTable.class) return table;
		if (target == DeXArray.class) return table.values();
				
		return StaticSerialization.get(target).deserialize(table);
	}
	
	/** Check if the supplied object is primitve, in hopefully decreasing order of popularity */
	public static boolean isPrimitive(Object o) {
		return o instanceof String 
			|| o instanceof Integer
			|| o instanceof Boolean
			|| o instanceof Double 
			|| o instanceof Float
			|| o instanceof Byte
			|| o instanceof Short
			|| o instanceof Long
			|| o instanceof Character;
	}
	
	public static Collection<?> tableToCollection(DeXTable table) {
		return table.values();
	}
	
	public static HashMap<?, ?> tableToMap(DeXTable table) {
		if (table == null) return null;
		return table.toHashMap();
	}
	
	public static DeXTable arrayToTable(Object[] array, SerializationMap sel) {
		DeXTable.Builder builder = DeXTable.builder("", array.length);
		for(int i = 0; i < array.length; i++) {
			builder.add(toDeX(array[i], sel));
		}
		return builder.create();
	}
	
	public static DeXTable iterableToTable(Iterable<?> iterable, SerializationMap sel) {
		// We can't predict how long this thing is going to be
		DeXTable.Builder builder = DeXTable.builder("", 16);
		Iterator<?> iterator = iterable.iterator();
		
		while (iterator.hasNext()) {
			builder.add(toDeX(iterator.next(), sel));
		}
		
		return builder.create();
	}
	
	public static DeXTable mapToTable(Map<?, ?> map, SerializationMap sel) {
		DeXTable.Builder builder = DeXTable.builder("", map.size());
		for (Entry<?, ?> entry : map.entrySet()) {
			builder.put(toDeX(entry.getKey(), sel), toDeX(entry.getValue(), sel));
		}
		return builder.create();
	}
	
	public static DeXTable getByTag(DeXIterable<?> iterable, String tag) {
		for (Object o : iterable) {
			if (o instanceof DeXTable) {
				DeXTable table = (DeXTable) o;
				if (tag == null || table.tag().equals(tag)) return table;
			}
		}
		return null;
	}
	
	public static Collection<DeXTable> getAllByTag(DeXIterable<?> iterable, String tag) {
		Collection<DeXTable> ret = new LinkedList<>();
		for (Object o : iterable) {
			if (o instanceof DeXTable) {
				DeXTable table = (DeXTable) o;
				if (tag == null || table.tag().equals(tag)) {
					ret.add(table);
				}
			}
		}
		return ret;
	}
	
	public static String prettyPrint(DeXTable table) {
		StringBuilder builder = new StringBuilder();
		prettyPrint(table, builder, 2);
		return builder.toString();
	}
	
	private static void prettyPrint(DeXTable table, StringBuilder sb, int level) {
		boolean array = table.isArray();
		if (table.hasTag()) sb.append(table.tag).append(" { ");
		else sb.append("{ ");
		if (!array) sb.append('\n');
		
		boolean first = true;
		for (Entry<Object, Object> entry : table.entrySet()) {
			if (!array && level > 0) for (int j = 0; j < level; j++) sb.append(" ");
			if (!first && array) {
				sb.append(", ");
			} else first = false;
			
			if (!array) {
				if (entry.getKey() instanceof DeXTable) {
					prettyPrint((DeXTable) entry.getKey(), sb, level + 2);
				} else print(entry.getKey(), sb, true);
				sb.append(" : ");
			}
			if (entry.getValue() instanceof DeXTable) {
				prettyPrint((DeXTable) entry.getValue(), sb, level + 2);
			} else print(entry.getValue(), sb, true);
			
			if (!array) sb.append('\n');
		}
		if (!array) {
			if (level > 0) for (int j = 0; j < level - 2; j++) sb.append(" "); 
		} else sb.append(' ');
		
		sb.append('}');
	}
	
	public static String print(DeXTable table) {
		StringBuilder builder = new StringBuilder();
		print(table, builder);
		return builder.toString();
	}
	
	public static void print(DeXTable table, StringBuilder sb) {
		boolean array = table.isArray();
		if (table.hasTag()) sb.append(table.tag).append("{");
		else sb.append("{");
		
		boolean first = true;
		for (Entry<Object, Object> entry : table.entrySet()) {
			if (!first && array) {
				sb.append(",");
			} else first = false;
			
			if (!array) {
				if (entry.getKey() instanceof DeXTable) {
					print((DeXTable) entry.getKey(), sb);
				} else print(entry.getKey(), sb, false);
				sb.append(":");
			}
			if (entry.getValue() instanceof DeXTable) {
				print((DeXTable) entry.getValue(), sb);
			} else print(entry.getValue(), sb, false);
		}
		sb.append('}');
	}
	
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.#");
	
	private static void print(Object o, StringBuilder sb, boolean pretty) {
		if (o instanceof String) {
			String s = (String) o;
			sb.ensureCapacity(sb.length() + s.length());
			int start = sb.length();
			
			if (!pretty) sb.append('"');
			
			boolean isValid = pretty;
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				String s2 = escape(c);
				if (s2 != null) {
					sb.append(s2);
					isValid = false;
					continue;
				} else if (isValid) {
					switch (c) {
					case '{': isValid = false; break;
					case '}': isValid = false; break;
					case ':': isValid = false; break;
					case ',': isValid = false; break;
					}
				}
				sb.append(c);
			}
			
			if (!pretty) sb.append('"');
			else if (!isValid) {
				// Since we haven't inserted that one before we have to do this
				sb.insert(start, '"');
				sb.append('"');
			}
		} else {
			if (o instanceof Number) {
				sb.append(DOUBLE_FORMAT.format(((Number)o).doubleValue()));
			} else sb.append(o.toString());
		}
	}
			
	private static String escape(char c) {
		switch (c) {
	      case '\"': return "\\\"";
	      case '\t': return "\\t";
	      case '\n': return "\\n";
	      case '\r': return "\\r";
	      case '\f': return "\\f";
	      case '\b': return "\\b";
	      case '\\': return "\\\\";
		}
		return null;
	}
	
	// Static values for parsing, sadly we can't use builtin methods because they don't support non decimal fractions
	private static final char CHARS_NAN[] = "NaN".toCharArray();
	private static final char CHARS_INFINITY[] = "Infinity".toCharArray();
	
	public static Double parseDeXNumber(String s) {
		int i = 0;
		char[] chars = s.toCharArray();
		
		// Those are all things that can only happen once
		boolean hasSign = false;
		boolean hasExponent = false;
		boolean hasFractional = false;
		
		if (chars[i] == '-') {
			hasSign = true;
			i++;
		} else if (chars[i] == '+') i++; //+ Are generally ignored, why'd you need them anyways?
		
		// Check for NaN & Infinity
		if (chars[i] == 'N') {
			int j = 0;
			for (; i < chars.length; j++, i++) {
				if (j >= CHARS_NAN.length) return null;
				if (chars[i] != CHARS_NAN[j]) return null;
			}
			if (j < CHARS_NAN.length - 1) return null;
			return Double.NaN;
		}
		if (chars[i] == 'I') {
			int j = 0;
			for (; i < chars.length; j++, i++) {
				if (j >= CHARS_INFINITY.length) return null;
				if (chars[i] != CHARS_INFINITY[j]) return null;
			}
			if (j < CHARS_INFINITY.length - 1) return null;
			return hasSign ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		}
		
		// Check radix
		int radix = 10;
		if (chars[i] == '0') {
			// This is a 0
			if (i + 1 == chars.length) return 0D;
			
			switch (chars[i + 1]) {
			case 'x': radix = 16; i++; break;
			case 'o': radix = 8; i++; break;
			case 'b': radix = 2; i++;
			}
		}
		
		double result = 0;
		long fraction = 0;
		int exponent = 0;
		
		for (; i < chars.length; i++) {
			char c = chars[i];
			if (c == '.') {
				hasFractional = true; i++; break;
			}
			if (c == 'e' || c == 'E') {
				// This is invalid
				if (radix == 2) return null;
				// We have an exponent
				if (radix == 10) {
					hasExponent = true; i++; break;
				}
				// If this is hexadecimal, we just continue as normal
			}
			int digit = Character.digit(c, radix);
			// If this is an invalid digit, we don't contiue;
			if (digit == -1) return null;
			result = result * radix + digit;
		}
		if (hasSign) result = Math.copySign(result, -1);
		
		int fractionDigits = 0;
		
		if (hasFractional) {
			for (; i < chars.length; i++) {	
				char c = chars[i];
				// Check for exponents, once again
				if (c == 'e' || c == 'E') {
					// This is invalid
					if (radix == 2) return null;
					// We have an exponent
					if (radix == 10) {
						hasExponent = true; i++; break;
					}
					// If this is hexadecimal, we just continue as normal
				}
				
				int digit = Character.digit(c, radix);
				// If this is an invalid digit, we don't contiue;
				if (digit == -1) return null;
				fraction = fraction * radix + digit;
				fractionDigits++;
			}
			
			// Add fraction
			result += fraction / (double)(Math.pow(radix, fractionDigits));
		}
		
		// The exponenet can have a different sign, so reset this one
		hasSign = false;
		if (hasExponent) {
			
			if (chars[i] == '-') {
				hasSign = true;
				i++;
			} else if (chars[i] == '+') i++; //+ Are generally ignored, why'd you need them anyways?
			
			for (; i < chars.length; i++) {	
				char c = chars[i];
				
				int digit = Character.digit(c, radix);
				// If this is an invalid digit, we don't contiue;
				if (digit == -1) return null;
				exponent = exponent * radix + digit;
			}
			
			if (hasSign) exponent = -exponent;
			// Add exponent
			result *= Math.pow(radix, exponent);
		}
		
		return result;
	}
}
