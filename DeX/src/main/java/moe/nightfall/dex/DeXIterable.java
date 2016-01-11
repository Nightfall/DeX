package moe.nightfall.dex;

import java.util.Collection;

interface DeXIterable<E> extends Iterable<Object> {

	Object get(E key);

	String tag();

	default boolean hasTag() {
		return tag().length() > 0;
	}

	/**
	 * Note that numbers will only be avaialable via {@link Double.class} and
	 * {@link Long.class} if this was deserialized, use the respective getters.
	 * 
	 * @param type
	 * @param key
	 * @return
	 */
	default <T> T get(Class<T> type, E key) {
		return DeX.coerce(type, get(key));
	}

	default <T> T get(Class<T> type, E key, T def) {
		Object obj = get(key);
		if (obj == null)
			return def;
		else
			return DeX.coerce(type, obj);
	}

	default String getString(E key, String def) {
		return get(String.class, key, def);
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
		if (o instanceof Double || o instanceof Float) {
			long value = ((Number) o).longValue();
			// Don't round!
			if (!o.equals(value))
				throw new ArithmeticException(o + "can't be converted to integer!");;
			return value;
		} else if (o instanceof Number) return ((Number) o).longValue();
		throw new ArithmeticException("Table can't be converted to number!");
	}

	default long getLong(E key) {
		Object o = get(key);
		if (o == null) throw new NullPointerException();
		if (o instanceof Double || o instanceof Float) {
			long value = ((Number) o).longValue();
			// Don't round!
			if (!o.equals(value))
				throw new ArithmeticException(o + " can't be converted to integer!");;
			return value;
		} else if (o instanceof Number) return ((Number) o).longValue();
		throw new ArithmeticException("Table can't be converted to number!");
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
