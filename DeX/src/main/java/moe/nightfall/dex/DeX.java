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
		return toDeX(in, null);
	}
	
	public static Object toDeX(Object in, SerializationMap sel) {
		if (isPrimitive(in)) return in;
		
		if (in instanceof Object[]) return arrayToTable((Object[])in);
		if (in instanceof Iterable) return iterableToTable((Iterable<?>)in);
		if (in instanceof Map) return mapToTable((Map<?, ?>)in);
		
		// Sanity check
		if (in instanceof DeXTable) return in;
		if (in instanceof DeXArray) return ((DeXArray)in).toDeXTable();
		
		String tag = "";
		if (sel != null) {
			tag = sel.tagFor(in.getClass());
		}
		
		// Raw types because generics are too dump to handle this
		return StaticSerialization.get((Class)in.getClass()).serialize(in, tag);
	}
	
	public static Object toJava(Class<?> target, DeXTable in) {
		
		// Note that we can only deserialize to some default type. The method above can serialize anything.
		if (target.isAssignableFrom(HashMap.class)) return in.copy();
		
		// We can cope with the most basic collections
		// TODO What about Vector? Or is that thing even used nowadays?
		if (target.isAssignableFrom(ArrayList.class)) return new ArrayList<>(tableToCollection(in));
		if (target.isAssignableFrom(LinkedList.class)) return new LinkedList<>(tableToCollection(in));
		if (target.isAssignableFrom(HashSet.class)) return new HashSet<>(tableToCollection(in));
		
		if (target.isArray()) return tableToCollection(in).toArray();
		
		if (target.isAssignableFrom(HashMap.class)) return tableToMap(in);
			
		// Sanity check
		if (target == DeXTable.class) return in;
		if (target == DeXArray.class) return in.values();
				
		return StaticSerialization.get(target).deserialize(in);
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
	
	public static DeXTable arrayToTable(Object[] array) {
		DeXTable table = new DeXTable(array.length, "");
		for(int i = 0; i < array.length; i++) {
			table.append(i, toDeX(array[i]));
		}
		return table;
	}
	
	public static DeXTable iterableToTable(Iterable<?> iterable) {
		// We can't predict how long this thing is going to be
		DeXTable table = new DeXTable(0, "");
		Iterator<?> iterator = iterable.iterator();
		
		int i = 0;
		while (iterator.hasNext()) {
			i++;
			table.append(i, toDeX(iterator.next()));
		}
		
		return table;
	}
	
	public static DeXTable mapToTable(Map<?, ?> map) {
		return new DeXTable(map);
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
}
