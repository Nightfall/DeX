package moe.nightfall.dex;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import moe.nightfall.dex.DeXSerializable.Serialization;

/**
 * Immutable table
 * 
 * @author Vic
 */
public final class DeXTable extends AbstractMap<Object, Object> implements DeXIterable<Object> {

	private Set<Entry<Object, Object>> underlying;
	private DeXArray array;
	private boolean isArray = true;
	
	private final String tag;
	
	public static DeXTable create(Map<?, ?> m) {
		return create(m, "");
	}
	
	public static DeXTable create(Map<?, ?> m, String tag) {
		return builder(tag, m.size()).addAll(m).create();
	}
	
	public static DeXTable create(Iterable<?> i) {
		return create(i, "");
	}
	
	public static DeXTable create(Iterable<?> i, String tag) {
		return builder(tag).addAll(i).create();
	}
	
	public static DeXTable create(Object... array) {
		return builder().addAll(array).create();
	}
	
	/** Internal constructor for TableBuilder */
	private DeXTable(int size, String tag) {
		this.tag = tag;
		underlying = new LinkedHashSet<>(size);
	}
	
	public static Builder builder(String tag, int size) { return new Builder(size, tag); }
	public static Builder builder(int size) { return new Builder(size, ""); }
	public static Builder builder(String tag) { return new Builder(16, tag); }
	public static Builder builder() { return new Builder(16, ""); }
	
	public static class Builder {
		
		private DeXTable table;
	
		private Builder(int size, String tag) {
			table = new DeXTable(size, tag);
		}
		
		public Builder put(Object key, Object value) {
			if (table == null) throw new IllegalStateException("Builder finished!");
			int i = table.size();
			if (key == null || value == null)
				throw new IllegalArgumentException("DeXTable doesn't allow null keys or values!");
			
			// Coerce numbers to double for comparision
			if (key instanceof Number) key = ((Number)key).doubleValue();
			if (value instanceof Number) value = ((Number)value).doubleValue();
			
			if (table.isArray) 
				table.isArray = key instanceof Number && ((Number)key).doubleValue() == i;
			table.underlying.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
			return this;
			
		}
		
		public Builder add(Object value) {
			if (table == null) throw new IllegalStateException("Builder finished!");
			if (value == null) 
				throw new IllegalArgumentException("DeXTable doesn't allow null values!");
			
			if (value instanceof Number) value = ((Number)value).doubleValue();
			// This has to use a double as key for comparision
			table.underlying.add(new AbstractMap.SimpleImmutableEntry<>((double) table.size(), value));
			return this;
		}
		
		public Builder addAll(Iterable<?> iterable) {
			for (Object o : iterable) add(o);
			return this;
		}
		
		public Builder addAll(Object... values) {
			for (Object o : values) add(o);
			return this;
		}
		
		public Builder addAll(Map<?, ?> map) {
			for (Entry<?, ?> entry : map.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
			return this;
		}
		
		public DeXTable create() {
			DeXTable table = this.table;
			this.table = null;
			return table;
		}
	}
	
	@Override
	public int size() {
		return underlying.size();
	}

	@Override
	public Object get(Object key) {
		// If this is a number we have to convert it to double
		if (key instanceof Number) key = ((Number)key).doubleValue();
		return super.get(key);
	}

	@Override
	public Iterator<Object> iterator() {
		return values().iterator();
	}
	
	@Override
	public String tag() {
		return tag;
	}
	
	@Override
	public Set<Entry<Object, Object>> entrySet() {
		return underlying;
	}

	@Override
	public DeXArray values() {
		if (array == null)
			array = new DeXArray(this, super.values());
		return array;
	}
	

	/**
	 * Returns true if this is an array, means it complies with
	 * starting with index {@code 0} and ending with index {@link #size()}{@code - 1}.
	 * 
	 * It is recommended to always use {@link #toDeXArray()} if you want
	 * array indexing.
	 * @return
	 */
	public boolean isArray() {
		return isArray;
	}
	
	/** This is just for convenience concerning the naming... */
	public DeXArray toDeXArray() {
		return values();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof DeXIterable) {
			DeXIterable<?> di = (DeXIterable<?>) o;
			if (!Objects.equals(tag(), di.tag())) 
				return false;
		}
		return super.equals(o);
	}

	public <T> T compose(Class<T> target, Serialization sel) {
		return (T) DeX.compose(target, this, sel);
	}
	
	/** This returns a copy of the table as Java HashMap */
	public HashMap<Object, Object> toHashMap() {
		return new HashMap<>(this);
	}
	
	@Override
	public String toString() {
		return DeX.prettyPrint(this);
	}
}
