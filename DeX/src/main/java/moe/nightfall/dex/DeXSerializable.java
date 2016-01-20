package moe.nightfall.dex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public interface DeXSerializable {
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface DeXDeserializer {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface DeXSerializer {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface Serialize {}
	
	public static class SerializationMap {
		
		/** 
		 * Use this for an immutable, empty serialization map in case you don't want
		 * custom tag serialization
		 */
		public static final SerializationMap empty = new SerializationMap() {			
			@Override 
			public void put(String tag, Class<?> clazz) { 
				throw new UnsupportedOperationException(); 
			}
			
			@Override 
			public <T> void put(String tag, Class<T> clazz, Serializer<T> sel) { 
				throw new UnsupportedOperationException(); 
			}
		};
		
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
					byClass.put(clazz, sel = new DeXSerializerImpl(clazz));
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
		
		/** 
		 * <p>
		 * Override this to serialize a given Object.
		 * By convention the returned table has the tag
		 * defined in the given {@link SerializationMap} for
		 * the class of obj.
		 * </p><p>
		 * Use it like this: {@code 
		 * 		return new DeXTable(map, sel.tagFor(Class));
		 * }
		 * </p>
		 */
		public DeXTable serialize(T obj, SerializationMap sel);
		
		/**
		 * Override this to deserialze a given {@link DeXTable}.
		 */
		public T deserialize(DeXTable table);
	}
	
	// TODO Use java 8 lambdas for faster execution speed?
	static class ClassSerializer<T> implements Serializer<T> {
		
		protected Field[] fields;
		protected final Class<T> clazz;
		
		ClassSerializer(Class<T> clazz) {
			try {
				clazz.getConstructor();
			} catch (Exception e) {
				throw new RuntimeException("Can not serialize class " + clazz + ", must have an empty constructor!");
			}
			this.clazz = clazz;
			fields = genFields();
		}
		
		protected Field[] genFields() {
			return Arrays.stream(clazz.getDeclaredFields()).filter(
				field -> !(Modifier.isStatic(field.getModifiers()) 
						|| Modifier.isFinal(field.getModifiers()))
			).toArray(Field[]::new);
		}

		@Override
		public DeXTable serialize(T obj, SerializationMap map) {
			DeXTable.Builder builder = DeXTable.builder(map.tagFor(clazz), fields.length);
			for (Field f : fields) {
				try {
					if (!f.isAccessible()) f.setAccessible(true);
					builder.put(f.getName(), DeX.decompose(f.get(obj), map));
				} catch (Exception e) {
					throw new RuntimeException("Error while trying to serialize Object: ", e);
				}
			}
			return builder.create();
		}

		@Override
		public T deserialize(DeXTable table) {
			try {
				T obj = clazz.newInstance();
				for (Field f : fields) {
					if (!f.isAccessible()) f.setAccessible(true);
					f.set(obj, DeX.compose(f.getType(), table.get(f.getName())));
				}
				return obj;
			} catch (Exception e) {
				throw new RuntimeException("Error while trying to deserialize from table: ", e);
			}
		}
	}
	
	static class DeXSerializerImpl<T extends DeXSerializable> extends ClassSerializer<T> {
		
		private Method serialize;
		private Method deserialize;
		
		DeXSerializerImpl(Class<T> clazz) {
			super(clazz);
			for (Method m : clazz.getDeclaredMethods()) {
				int modifiers = m.getModifiers();
				if (m.getAnnotation(DeXSerializer.class) != null) {
					if (serialize != null) throw new RuntimeException("Second serializer found in the same class!");
					if (Modifier.isPublic(modifiers)) {
						if (Arrays.equals(m.getParameters(), new Class[] { clazz, SerializationMap.class })) serialize = m;
						else throw new RuntimeException("Couldn't create serializer for class " + clazz + ", wrong argument types!");
					} else throw new RuntimeException("Couldn't create serializer for class " + clazz + ", method is not public!");
				} else if (m.getAnnotation(DeXDeserializer.class) != null) {
					if (deserialize != null) throw new RuntimeException("Second deserializer found in the same class!");
					if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
						if (Arrays.equals(m.getParameters(), new Class[] { DeXTable.class })) serialize = m;
						else throw new RuntimeException("Couldn't create deserializer for class " + clazz + ", wrong argument types!");
					} else throw new RuntimeException("Couldn't create deserialzer for class " + clazz + ", method is not public static!");
				}
			}
		}
		
		@Override 
		protected Field[] genFields() {
			return Arrays.stream(clazz.getDeclaredFields()).filter(
					field -> !(field.getAnnotation(Serialize.class) == null 
							|| Modifier.isStatic(field.getModifiers()) 
							|| Modifier.isFinal(field.getModifiers()))
			).toArray(Field[]::new);
		}

		@Override
		public DeXTable serialize(T obj, SerializationMap sel) {
			if (serialize != null) {
				try {
					return (DeXTable) serialize.invoke(obj, sel);
				} catch (Exception e) {
					throw new RuntimeException("Error while trying to serialize Object: ", e);
				}
			} else return super.serialize(obj, sel);
		}

		@Override
		public T deserialize(DeXTable table) {
			if (deserialize != null) {
				try {
					return (T) deserialize.invoke(null, table);
				} catch (Exception e) {
					throw new RuntimeException("Error while trying to serialize Object: ", e);
				}
			} else return super.deserialize(table);
		}
	}
}
