package moe.nightfall.dex;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import moe.nightfall.dex.serialize.DeXSerializable.Serializer;
import moe.nightfall.dex.serialize.Serialization;

public final class DeX {
	private DeX() {}
	
	@SuppressWarnings("unchecked")
	public static <T> T coerce(Class<T> target, Object o, Serialization sel) {
		if (o == null) return null;
		if (target.isInstance(o)) {
			return (T) o;
		}
		if (CharSequence.class.isAssignableFrom(target)) {
			// Try to coerce strings
			if (o instanceof Number) {
				return (T) DOUBLE_FORMAT.format(((Number)o).doubleValue());
			} else if (o instanceof Boolean) {
				return (T) o.toString();
			}
		}
		
		if (o instanceof DeXTable && sel != null) {
			return sel.forClass(target).deserialize((DeXTable) o, sel);
		}
		throw new IllegalArgumentException(o + " cant be coerced into " + target);
	}
	
	@SuppressWarnings("unchecked")
	// FIXME This in unchecked and I probably can't tell if it fails due to an incorrect
	// cast, might want to add a class parameter to this
	public static <T> T deserialize(DeXTable table, Serialization sel) {
		return (T) sel.forTable(table).deserialize(table, sel);
	}
	
	// Used for serialization
	
	public static Object decompose(Object in, Serialization sel) {
		if (in == null) return null;
		if (isPrimitive(in)) return in;
		
		if (in instanceof Object[]) return arrayToTable((Object[])in, sel);
		if (in instanceof Iterable) return iterableToTable((Iterable<?>)in, sel);
		if (in instanceof Map) return mapToTable((Map<?, ?>)in, sel);
		
		if (in instanceof DeXArray) return mapToTable(((DeXArray)in).toDeXTable(), sel);

		// Raw types because generics are too dump to handle this
		return sel.forClass((Class)in.getClass()).serialize(in, sel);
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
	
	public static Object compose(Object in, Serialization sel) {
		if (in instanceof DeXTable) {
			DeXTable table = (DeXTable) in;
			if (table.hasTag()) {
				Serializer<?> ser = sel.forTag(table.tag());
				if (ser != null) return ser.deserialize(table, sel);
			}
		}
		return in;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> Class<T> wrap(Class<T> c) {
		return c.isPrimitive() ? (Class<T>) PRIMITIVE_WRAPPERS.get(c) : c;
	}

	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<>();
	private static final Map<Class<?>, Function<Number, ?>> NUMBER_MAPPERS = new HashMap<>();
	
	static {
		primitiveMapper(byte.class, Byte.class, Number::byteValue);
		primitiveMapper(double.class, Double.class, Number::doubleValue);
		primitiveMapper(float.class, Float.class, Number::floatValue);
		primitiveMapper(int.class, Integer.class, Number::intValue);
		primitiveMapper(long.class, Long.class, Number::longValue);
		primitiveMapper(short.class, Short.class, Number::shortValue);
		
		PRIMITIVE_WRAPPERS.put(char.class, Character.class);
		PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
	}
	
	private static <T> void primitiveMapper(Class<?> primitive, Class<T> wrapper, Function<Number, T> mapper) {
		PRIMITIVE_WRAPPERS.put(primitive, wrapper);
		NUMBER_MAPPERS.put(wrapper, mapper);
	}
	
	/**
	 * This is used for deserialization
	 */
	public static Object compose(Class<?> target, Object in, Serialization sel) {
		
		if (in == null) return null;
		target = wrap(target);
		if (in instanceof Number) {
			return NUMBER_MAPPERS.get(target).apply((Number) in);
		}
		if (in instanceof String) return in;
		
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
		
		if (target == DeXTable.class) return table;
		if (target == DeXArray.class) return table.values();
				
		return sel.forClass(target).deserialize(table, sel);
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
	
	public static DeXTable arrayToTable(Object[] array, Serialization sel) {
		DeXTable.Builder builder = DeXTable.builder("", array.length);
		for(int i = 0; i < array.length; i++) {
			builder.add(decompose(array[i], sel));
		}
		return builder.create();
	}
	
	public static DeXTable iterableToTable(Iterable<?> iterable, Serialization sel) {
		// We can't predict how long this thing is going to be
		DeXTable.Builder builder = DeXTable.builder("", 16);
		Iterator<?> iterator = iterable.iterator();
		
		while (iterator.hasNext()) {
			builder.add(decompose(iterator.next(), sel));
		}
		
		return builder.create();
	}
	
	public static DeXTable mapToTable(Map<?, ?> map, Serialization sel) {
		DeXTable.Builder builder = DeXTable.builder("", map.size());
		for (Entry<?, ?> entry : map.entrySet()) {
			builder.put(decompose(entry.getKey(), sel), decompose(entry.getValue(), sel));
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
		if (table.hasTag()) {
			print(table.tag(), sb, true);
			sb.append(" { ");
		} else sb.append("{ ");
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
	
	public static String print(DeXTable table, boolean pretty) {
		if (pretty) return prettyPrint(table);
		else return print(table);
	}
	
	public static String print(DeXTable table) {
		StringBuilder builder = new StringBuilder();
		print(table, builder);
		return builder.toString();
	}
	
	public static void print(DeXTable table, StringBuilder sb) {
		boolean array = table.isArray();
		if (table.hasTag()) {
			print(table.tag(), sb, false);
		}
		sb.append("{");
		
		boolean first = true;
		for (Entry<Object, Object> entry : table.entrySet()) {
			if (!first) {
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
	
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	
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
	
	public static Double parseDeXNumber(String s) {
		int i = 0;
		char[] chars = s.toCharArray();
		
		boolean hasSign = false;
		
		if (chars[i] == '-') {
			hasSign = true;
			i++;
		} else if (chars[i] == '+') i++; //+ Are generally ignored, why'd you need them anyways?
		
		// Check radix
		int radix = 10;
		if (chars[i] == '0') {
			// This is a 0
			if (i + 1 == chars.length) return 0D;
			
			switch (chars[i + 1]) {
			case 'x': radix = 16; i += 2; break;
			case 'o': radix = 8; i += 2; break;
			case 'b': radix = 2; i += 2;
			}
		}
		
		String number = (hasSign ? "-" : "") + s.substring(i);
		try {
			if (radix == 10) {
				return Double.parseDouble(number);
			} else {
				return Long.valueOf(number, radix).doubleValue();
			}
		} catch (Exception e) {
			// Invalid number
			return null;
		}
	}
}
