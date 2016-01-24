package moe.nightfall.dex.serialize;

import java.util.HashMap;
import java.util.Map;

import moe.nightfall.dex.DeXTable;
import moe.nightfall.dex.serialize.DeXSerializable.Serializer;

public class Serialization {
	
	private Map<String, Serializer<?>> serializerByTag = new HashMap<>();
	// Reverse lookup for serialization
	private Map<Class<?>, String> tagByClass = new HashMap<>();
	private Map<Class<?>, Serializer<?>> serializerByClass = new HashMap<>();
	
	public void serializeTagAs(String tag, Class<?> clazz) {
		Serializer<?> ser = forClass(clazz);
		serializerByTag.put(tag, ser);
		tagByClass.put(clazz, tag);
	}
	
	public <T> void serializeTagAs(String tag, Class<T> clazz, Serializer<T> ser) {
		serializeAs(clazz, ser);
		serializerByTag.put(tag, ser);
		tagByClass.put(clazz, tag);
	}
	
	public <T> void serializeAs(Class<T> clazz, Serializer<T> ser) {
		serializerByClass.put(clazz, ser);
	}
	
	public Serializer<?> forTable(DeXTable table) {
		if (!table.hasTag()) throw new IllegalArgumentException("Can not deserialize table, no tag defined");
		Serializer<?> sel = forTag(table.tag());
		if (sel == null) throw new IllegalArgumentException("Can not deserialize table, no serializer defined for tag " + table.tag());
		return sel;
	}
	
	public Serializer<?> forTag(String tag) {
		return serializerByTag.get(tag);
	}
	
	public String tagFor(Class<?> clazz) {
		return tagByClass.getOrDefault(clazz, "");
	}
	
	public <T> Serializer<T> forClass(Class<T> clazz) {
		Serializer<T> sel = (Serializer<T>) serializerByClass.get(clazz);
		if (sel == null) {
			if (DeXSerializable.class.isAssignableFrom(clazz)) {
				serializerByClass.put(clazz, sel = new DeXSerializerImpl(clazz));
			} else {
				serializerByClass.put(clazz, sel = new ClassSerializer(clazz));
			}
		}
		return sel;
	}
}