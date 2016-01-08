package moe.nightfall.dex;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

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
			return deserialize(target, (DeXTable)o);
		}
		throw new ClassCastException("Object " + o + " is not coerable into class " + target);
	}
	
	public static <T> T deserialize(Class<T> target, DeXTable table) {
		return null;
	}
	
	public static <T> T deserialize(DeXTable table) {
		if (table.parser == null) {
			throw new IllegalArgumentException("");
		}
		return null;
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
