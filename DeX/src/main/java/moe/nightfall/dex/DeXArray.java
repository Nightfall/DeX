package moe.nightfall.dex;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DeXArray extends AbstractList<Object> implements DeXIterable<Integer> {
	
	private List<Object> values;
	private final String tag;
	
	final DeXTable table;

	DeXArray(DeXTable parent, Collection<?> values) {
		this.values = new ArrayList<>(values);
		this.tag = parent.tag();
		this.table = parent;
	}
	
	@Override
	public String tag() {
		return tag;
	}
	
	// Fucking hell, java primitives
	@Override
	public Object get(Integer key) {
		if (key == null) throw new NullPointerException();
		return values.get(key);
	}
	
	@Override
	public Object get(int index) {
		return values.get(index);
	}

	@Override
	public int size() {
		return values.size();
	}
	
	public DeXTable toDeXTable() {
		return table;
	}
	
	@Override
	public String toString() {
		return DeX.prettyPrint(table);
	}
}