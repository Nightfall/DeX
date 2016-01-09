package moe.nightfall.dex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface DeXSerializable {
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface DeXDeserialize {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface DeXSerialize {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface DeX {}
	
	public static class SerializationMap {
		private Map<String, Serializer<?>> byTag = new HashMap<>();
		// Reverse lookup for serialization
		private Map<Class<?>, String> byClass = new HashMap<>();
		
		public void put(String tag, Class<?> clazz) {
			byTag.put(tag, StaticSerialization.get(clazz));
			byClass.put(clazz, tag);
		}
		
		public <T> void put(String tag, Class<T> clazz, Serializer<T> sel) {
			StaticSerialization.register(clazz, sel);
			byTag.put(tag, sel);
			byClass.put(clazz, tag);
		}
		
		public Serializer<?> byTable(DeXTable table) {
			if (!table.hasTag()) throw new IllegalArgumentException("Can not deserialize table, no tag defined");
			Serializer<?> sel = byTag(table.tag());
			if (sel == null) throw new IllegalArgumentException("Can not deserialize table, no serializer defined for tag " + table.tag());
			return sel;
		}
		
		public Serializer<?> byTag(String tag) {
			return byTag.get(tag);
		}
		
		public String tagFor(Class<?> clazz) {
			return byClass.getOrDefault(clazz, "");
		}
	}
	
	public static class StaticSerialization {
		private static Map<Class<?>, Serializer<?>> byClass = new HashMap<>();
		
		public static <T> Serializer<T> get(T o) {
			return (Serializer<T>) get(o.getClass());
		}
		
		public static <T> Serializer<T> get(Class<T> clazz) {
			Serializer<T> sel = (Serializer<T>) byClass.get(clazz);
			if (sel == null) {
				if (DeXSerializable.class.isAssignableFrom(clazz)) {
					byClass.put(clazz, sel = new DeXSerializier(clazz));
				} else {
					byClass.put(clazz, sel = new ClassSerializer(clazz));
				}
			}
			return sel;
		}
		
		public static <T> void register(Class<T> clazz, Serializer<T> serializer) {
			byClass.put(clazz, serializer);
		}
	}
	
	public static interface Serializer<T> {
		
		default DeXTable serialize(T obj, String tag) {
			DeXTable table = serialize(obj);
			table.tag = tag;
			return table;
		}
		
		/** 
		 * Override this to serialize a given Object. 
		 * Note that the tag may be overwritten by 
		 * {@link moe.nightfall.dex.DeX#toDeX(Object, SerializationMap)}}
		 */
		public DeXTable serialize(T obj);
		
		/**
		 * Override this to deserialze a given {@link DeXTable}.
		 */
		public T deserialize(DeXTable table);
	}
	
	static class ClassSerializer<T> implements Serializer<T> {
		ClassSerializer(Class<T> clazz) {
			
		}

		@Override
		public DeXTable serialize(T obj) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public T deserialize(DeXTable table) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	static class DeXSerializier<T extends DeXSerializable> implements Serializer<T> {
		
		private Method serialize;
		private Method deserialze;
		
		private Field[] fields;
		
		DeXSerializier(Class<T> clazz) {
			
		}

		@Override
		public DeXTable serialize(T obj) {
			if (serialize != null) {
				try {
					return (DeXTable) serialize.invoke(obj);
				} catch (Exception e) {
					throw new RuntimeException("Error while trying to serialize Object: ", e);
				}
			} else {
				Map<Object, Object> map = new HashMap<>(fields.length);
				for (Field f: fields) {
					
				}
			}
			return null;
		}

		@Override
		public T deserialize(DeXTable table) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
