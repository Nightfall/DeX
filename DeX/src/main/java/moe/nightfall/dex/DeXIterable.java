package moe.nightfall.dex;

import java.util.Collection;
import java.util.Optional;

interface DeXIterable<E> extends Iterable<Object> {
	
	Object get(E key);
	
	@SuppressWarnings("unchecked")
	default <T> Optional<T> rec(E key) {
		return (Optional<T>) Optional.of(get(key));
	}
	
	String tag();
	
	default boolean hasTag() {
		return tag().length() > 0;
	}
	
	/**
	 * Note that numbers will only be avaialable via {@link Double.class} and
	 * {@link Long.class}, use the respective getters.
	 * @param type
	 * @param key
	 * @return
	 */
	default <T> Optional<T> rec(Class<T> type, E key) {
		return Optional.ofNullable(DeX.coerce(type, get(key)));
	}
	
	// FIXME We need proper exceptions for error handling
	
	default String getString(E key, String def) {
		return rec(String.class, key).orElse(def);
	}
	
	default String getString(E key) {
		return rec(String.class, key).get();
	}
	
	default double getDouble(E key, double def) {
		return Optional.ofNullable((Double) get(key)).orElse(def).floatValue();
	}
	
	default double getDouble(E key) {
		return (Double) get(key);
	}
	
	default float getFloat(E key, float def) {
		return (float) getDouble(key, def);
	}
	
	default float getFloat(E key) {
		return (float) getDouble(key);
	}
	
	default long getLong(E key, long def) {
		return Optional.ofNullable((Long) get(key)).orElse(def).longValue();
	}
	
	default long getLong(E key) {
		return (Long) get(key);
	}
	
	default int getInt(E key, int def) {
		return (int) getLong(key, def);
	}
	
	default int getInt(E key) {
		return (int) getLong(key);
	}
	
	default byte getByte(E key, byte def) {
		return (byte) getLong(key, def);
	}
	
	default byte getByte(E key) {
		return (byte) getLong(key);
	}
	
	default short getShort(E key, short def) {
		return (short) getLong(key, def);
	}
	
	default short getShort(E key) {
		return (short) getLong(key);
	}
	
	default boolean getBoolean(E key, boolean def) {
		return Optional.ofNullable((Boolean) get(key)).orElse(def);
	}
	
	default boolean getBoolean(E key) {
		return (Boolean) get(key);
	}
	
	default char getChar(E key, char def) {
		String str = getString(key);
		if (str == null) return def;
		if (str.length() > 1) throw new RuntimeException("No char!");
		return str.charAt(0);
	}
	
	default char getChar(E key) {
		return getString(key).charAt(0);
	}

	default DeXTable getByTag(String tag) {
		return DeX.getByTag(this, tag);
	}

	default Collection<DeXTable> getAllByTag(String tag) {
		return DeX.getAllByTag(this, tag);
	}
}
