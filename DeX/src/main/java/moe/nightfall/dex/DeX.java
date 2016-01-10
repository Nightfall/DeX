package moe.nightfall.dex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
		} else if (CharSequence.class.isAssignableFrom(target)) {
			return (T) o.toString();
		} else if (o instanceof DeXTable) {
			if (DeXArray.class.isAssignableFrom(target)) return (T)((DeXTable)o).values();
			else return deserialize((DeXTable)o);
		}
		throw new ClassCastException("Object " + o + " is not coerable into class " + target);
	}
	
	@SuppressWarnings("unchecked")
	// FIXME This in unchecked and I probably can't tell if it fails due to an incorrect
	// cast, might want to add a class parameter to this
	public static <T> T deserialize(DeXTable table) {
		if (table.serialization == null) {
			throw new IllegalArgumentException("Couldn't deserialize table with the tag type of \"" + table.tag() + "\", no deserialization provided");
		}
		return (T) table.serialization.byTable(table).deserialize(table);
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
			DeXTable table = new DeXTable(1, "");
			table.append(0, in);
			return table;
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
		if (target.isAssignableFrom(HashMap.class)) return table.copy();
		
		// We can cope with the most basic collections
		// TODO What about Vector? Or is that thing even used nowadays?
		if (target.isAssignableFrom(ArrayList.class)) return new ArrayList<>(tableToCollection(table));
		if (target.isAssignableFrom(LinkedList.class)) return new LinkedList<>(tableToCollection(table));
		if (target.isAssignableFrom(HashSet.class)) return new HashSet<>(tableToCollection(table));
		
		if (target.isArray()) return tableToCollection(table).toArray();
		
		if (target.isAssignableFrom(HashMap.class)) return tableToMap(table);
			
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
		List<Object> list = new ArrayList<>(table.size());
		for (Object o : table.toDeXArray()) {
			// Try to automatically deserialize
			if (o instanceof DeXTable) 
				o = ((DeXTable) o).deserialize();
			list.add(o);
		}
		return list;
	}
	
	public static HashMap<?, ?> tableToMap(DeXTable table) {
		HashMap<Object, Object> map = new HashMap<>(table.size());
		for(Entry<Object, Object> entry : table.entrySet()) {
			Object k = entry.getKey();
			Object v = entry.getValue();
			
			// Try to automatically deserialize
			if (k instanceof DeXTable) 
				k = ((DeXTable) k).deserialize();
			if (v instanceof DeXTable) 
				v = ((DeXTable) v).deserialize();
			
			map.put(k, v);
		}
		return map;
	}
	
	public static DeXTable arrayToTable(Object[] array, SerializationMap sel) {
		DeXTable table = new DeXTable(array.length, "");
		for(int i = 0; i < array.length; i++) {
			table.append(i, toDeX(array[i], sel));
		}
		return table;
	}
	
	public static DeXTable iterableToTable(Iterable<?> iterable, SerializationMap sel) {
		// We can't predict how long this thing is going to be
		DeXTable table = new DeXTable(0, "");
		Iterator<?> iterator = iterable.iterator();
		
		int i = 0;
		while (iterator.hasNext()) {
			i++;
			table.append(i, toDeX(iterator.next(), sel));
		}
		
		return table;
	}
	
	public static DeXTable mapToTable(Map<?, ?> map, SerializationMap sel) {
		DeXTable table = new DeXTable(map.size(), "");
		for (Entry<Object, Object> entry : table.entrySet()) {
			table.append(toDeX(entry.getKey(), sel), toDeX(entry.getValue(), sel));
		}
		return table;
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
		return Collections.unmodifiableCollection(ret);
	}
	
	public static String prettyPrint(DeXTable table) {
		
	}
}
