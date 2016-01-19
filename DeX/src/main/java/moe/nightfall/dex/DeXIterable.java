package moe.nightfall.dex;

import java.util.Collection;

interface DeXIterable<E> extends Iterable<Object> {

	/**
	 * In order to comply with this interface, the implementation has to coerce
	 * any {@link Number} to a {@link Double}
	 * 
	 * @param key
	 * @return
	 */
	Object get(E key);

	String tag();
	
	String print();
	
	default String prettyPrint() {
		return toString();
	}

	default boolean hasTag() {
		return tag().length() > 0;
	}
	
	default boolean hasKey(E key) {
		return get(key) != null;
	}

	/**
	 * Note that numbers will only be avaialable via {@link Double.class}
	 * if this was deserialized, use the respective getters.
	 * 
	 * This method tries to coerce with {@link DeX#coerce(Class, Object)}
	 * 
	 * @param type
	 * @param key
	 * @return
	 */
	default <T> T get(Class<T> type, E key) {
		return DeX.coerce(type, get(key));
	}

	/**
	 * Same as {@link #get(Class, Object)}, but allows to pass a default value
	 * in case {@link #hasKey(Object)} returns false.
	 * 
	 * @param type
	 * @param key
	 * @param def
	 * @return
	 */
	default <T> T get(Class<T> type, E key, T def) {
		Object obj = get(key);
		if (obj == null)
			return def;
		return DeX.coerce(type, obj);
	}
	
	default DeXTable getTable(E key) {
		return (DeXTable) get(key);
	}

	default String getString(E key, String def) {
		Object obj = get(key);
		if (obj == null) return def;
		else if (obj instanceof Boolean || obj instanceof Double) 
			return obj.toString();
		throw new RuntimeException();
	}

	default String getString(E key) {
		return get(String.class, key);
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

	default DeXTable getByTag(String tag) {
		return DeX.getByTag(this, tag);
	}

	default Collection<DeXTable> getAllByTag(String tag) {
		return DeX.getAllByTag(this, tag);
	}
}
