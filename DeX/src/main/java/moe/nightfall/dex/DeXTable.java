package moe.nightfall.dex;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DeXTable extends AbstractMap<Object, Object> implements DeXIterable<Object> {

	private Set<Entry<Object, Object>> underlying;
	private DeXArray array;
	
	/** Parser reference, used for deserializaton of tags, might be null */
	DeXParser parser;
	
	private final String tag;
	
	public DeXTable(Map<Object, Object> m) {
		this(m, "");
	}
	
	public DeXTable(Map<Object, Object> m, String tag) {
		this.tag = tag;
		underlying = new LinkedHashSet<>(m.size());
		underlying.addAll(m.entrySet());
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
	
	public <T extends DeXSerializable> T deserialize(Class<T> target) {
		return DeX.deserialize(target, this);
	}
	
	public <T> T deserialize() {
		return DeX.deserialize(this);
	}
}
