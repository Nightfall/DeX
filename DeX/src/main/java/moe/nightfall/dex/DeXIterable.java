package moe.nightfall.dex;

import java.util.Collection;
import java.util.Optional;

public interface DeXIterable<E> extends Iterable<Object> {
	
	public Object get(E key);
	
	@SuppressWarnings("unchecked")
	public default <T> Optional<T> rec(E key) {
		return (Optional<T>) Optional.of(get(key));
	}
	
	public String tag();
	
	public default boolean hasTag() {
		return tag().length() > 0;
	}
	
	public default <T> Optional<T> rec(Class<T> type, E key) {
		return Optional.ofNullable(DeX.coerce(type, get(key)));
	}

	public default DeXTable getByTag(String tag) {
		return DeX.getByTag(this, tag);
	}

	public default Collection<DeXTable> getAllByTag(String tag) {
		return DeX.getAllByTag(this, tag);
	}
}
