package moe.nightfall.dex;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import moe.nightfall.dex.DeXSerializable.SerializationMap;

public final class DeXTable extends AbstractMap<Object, Object> implements DeXIterable<Object> {

	private Set<Entry<Object, Object>> underlying;
	private DeXArray array;
	
	/** Serialization reference, used for deserializaton of tags, might be null */
	SerializationMap serialization;
	
	/** This should not be modified for immutability reasons. The only exception is when serializing */
	String tag;
	
	public DeXTable(Map<?, ?> m) {
		this(m, "");
	}
	
	public DeXTable(Map<?, ?> m, String tag) {
		this.tag = tag;
		underlying = new LinkedHashSet<>(m.size());
		for (Entry<?, ?> entry : m.entrySet()) {
			underlying.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
		}
	}
	
	/** This converts arrays, index will be taken as map key **/
	DeXTable(DeXArray array) {
		this(arrayToMap(array));
	}
	
	private static Map<Object, Object> arrayToMap(DeXArray array) {
		Map<Object, Object> map = new HashMap<>();
		for (int i = 0; i < array.size(); i++) {
			map.put(i, array.get(i));
		}
		return map;
	}
	
	// Internal methods, do not use from outside since they break immutability!
	
	DeXTable(int size, String tag) {
		this.tag = tag;
		underlying = new LinkedHashSet<>(size);
	}
	
	void append(Object key, Object value) {
		underlying.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
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
			array = new DeXArray(this);
		return array;
	}
	
	/** This is just for convenience concerning the naming... */
	public DeXArray toDeXArray() {
		return values();
	}
	
	public <T extends DeXSerializable> T deserialize(Class<T> target) {
		return (T) DeX.toJava(target, this);
	}
	
	public <T> T deserialize() {
		return DeX.deserialize(this);
	}
	
	/** This returns a copy of the table as Java HashMap */
	public HashMap<Object, Object> copy() {
		return new HashMap<>(this);
	}
}
