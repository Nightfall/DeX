package moe.nightfall.dex;

import java.util.Collection;

import moe.nightfall.dex.serialize.Serialization;

interface DeXIterable<E> extends Iterable<Object> {

	/**
	 * <p>In order to comply with this interface, the implementation has to coerce
	 * any {@link Number} to a {@link Double}.</p>
	 * 
	 * <p>Use {@link #rec(Object)} for a cast free alternative.</p>
	 * 
	 * @param key
	 * @return
	 */
	Object get(E key);

	String tag();

	default boolean hasTag() {
		return tag().length() > 0;
	}
	
	default boolean hasKey(E key) {
		return get(key) != null;
	}
	
	/** 
	 * This method tries to autocast, note that you can
	 * only use this with {@link Double} as the only primitive.
	 * Use the respective getters.
	 * 
	 * @param key
	 * @return
	 */
	default <T> T rec(E key) {
		return (T) get(key);
	}

	/**
	 * Note that numbers will only be avaialable via {@link Double.class}
	 * if this was deserialized, use the respective getters.
	 * 
	 * This method tries to coerce with {@link DeX#coerce(Class, Object, Serialization)}
	 * 
	 * @param type
	 * @param key
	 * @return
	 */
	default <T> T get(Class<T> type, Serialization sel, E key) {
		return DeX.coerce(type, get(key), sel);
	}

	/**
	 * Same as {@link #get(Class, Object, Serialization)}, but allows to pass a default value
	 * in case {@link #hasKey(Object)} returns false.
	 * 
	 * @param type
	 * @param key
	 * @param def
	 * @return
	 */
	default <T> T get(Class<T> type, Serialization sel, E key, T def) {
		Object obj = get(key);
		if (obj == null)
			return def;
		return DeX.coerce(type, obj, sel);
	}
	
	default DeXTable getTable(E key) {
		return (DeXTable) get(key);
	}
	
	default DeXArray getArray(E key) {
		DeXTable table = getTable(key);
		if (table != null && table.isArray()) return table.toDeXArray();
		else return null;
	}

	default String getString(E key, String def) {
		return get(String.class, null, key, def);
	}

	default String getString(E key) {
		return get(String.class, null, key);
	}

	default double getDouble(E key, double def) {
		Object o = get(key);
		if (o == null) return def;
		if (o instanceof Number) return ((Number) o).doubleValue();
		throw new ArithmeticException("Table can't be converted to number!");
	}

	default double getDouble(E key) {
		Object o = get(key);
		if (o == null) throw new NullPointerException();
		if (o instanceof Number) return ((Number) o).doubleValue();
		throw new ArithmeticException("Table can't be converted to number!");
	}

	default float getFloat(E key, float def) {
		return (float) getDouble(key, def);
	}

	default float getFloat(E key) {
		return (float) getDouble(key);
	}

	default long getLong(E key, long def) {
		Object o = get(key);
		if (o == null) return def;
		if (o instanceof Number) {
			double d = ((Number) o).doubleValue();
			if (d % 1 == 0) return (long) d;
			else throw new ArithmeticException("Double can't be converted to integer without loss!");
		} else throw new ArithmeticException("Table can't be converted to number!");
	}

	default long getLong(E key) {
		Object o = get(key);
		if (o == null) throw new NullPointerException();
		if (o instanceof Number) {
			double d = ((Number) o).doubleValue();
			if (d % 1 == 0) return (long) d;
			else throw new ArithmeticException("Double can't be converted to integer without loss!");
		} else throw new ArithmeticException("Table can't be converted to number!");
	}

	default int getInt(E key, int def) {
		long l = getLong(key, def);
		if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) 
			throw new ArithmeticException(l + " can't be converted to int!");
		return (int) l;
	}

	default int getInt(E key) {
		long l = getLong(key);
		if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) 
			throw new ArithmeticException(l + " can't be converted to int!");
		return (int) l;
	}

	default byte getByte(E key, byte def) {
		long l = getLong(key, def);
		if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE) 
			throw new ArithmeticException(l + " can't be converted to byte!");
		return (byte) l;
	}

	default byte getByte(E key) {
		long l = getLong(key);
		if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE) 
			throw new ArithmeticException(l + " can't be converted to byte!");
		return (byte) l;
	}
	
	default short getShort(E key, byte def) {
		long l = getLong(key, def);
		if (l > Short.MAX_VALUE || l < Short.MIN_VALUE) 
			throw new ArithmeticException(l + " can't be converted to short!");
		return (short) l;
	}

	default short getShort(E key) {
		long l = getLong(key);
		if (l > Short.MAX_VALUE || l < Short.MIN_VALUE) 
			throw new ArithmeticException(l + " can't be converted to short!");
		return (short) l;
	}

	default boolean getBoolean(E key, boolean def) {
		Boolean b = (Boolean) get(key);
		if (b == null) return def;
		return b;
	}

	default boolean getBoolean(E key) {
		return (boolean) get(key);
	}

	default char getChar(E key, char def) {
		String str = getString(key);
		if (str == null)
			return def;
		if (str.length() > 1)
			throw new RuntimeException("No char!");
		return str.charAt(0);
	}

	default char getChar(E key) {
		return getString(key).charAt(0);
	}

	// TODO You can't return tables if they've been serialized, baka
	default DeXTable getByTag(String tag) {
		return DeX.getByTag(this, tag);
	}

	default Collection<DeXTable> getAllByTag(String tag) {
		return DeX.getAllByTag(this, tag);
	}
}
