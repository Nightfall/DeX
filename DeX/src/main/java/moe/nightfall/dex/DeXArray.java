package moe.nightfall.dex;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class DeXArray extends AbstractList<Object> implements DeXIterable<Integer> {
	
	private List<Object> values;
	private final String tag;

	DeXArray(DeXTable underlying) {
		this.values = new ArrayList<>(values);
		this.tag = underlying.tag();
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
}