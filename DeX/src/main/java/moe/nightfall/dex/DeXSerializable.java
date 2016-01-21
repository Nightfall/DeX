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
	
	public static class Serialization {
		
		/** 
		 * Use this for an immutable, empty serialization map in case you don't want
		 * custom tag serialization
		 */
		public static final Serialization empty = new Serialization() {			
			@Override 
			public void put(String tag, Class<?> clazz) { 
				throw new UnsupportedOperationException(); 
			}
			
			@Override 
			public <T> void put(String tag, Class<T> clazz, Serializer<T> sel) { 
				throw new UnsupportedOperationException(); 
			}
		};
		
		private Map<String, Serializer<?>> serializerByTag = new HashMap<>();
		// Reverse lookup for serialization
		private Map<Class<?>, String> tagByClass = new HashMap<>();
		private Map<Class<?>, Serializer<?>> serializerByClass = new HashMap<>();
		
		public void put(String tag, Class<?> clazz) {
			Serializer<?> ser = forClass(clazz);
			serializerByTag.put(tag, ser);
			tagByClass.put(clazz, tag);
		}
		
		public <T> void put(String tag, Class<T> clazz, Serializer<T> ser) {
			serializerByClass.put(clazz, ser);
			serializerByTag.put(tag, ser);
			tagByClass.put(clazz, tag);
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
	
	public static interface Serializer<T> {
		
		/** 
		 * <p>
		 * Override this to serialize a given Object.
		 * By convention the returned table has the tag
		 * defined in the given {@link Serialization} for
		 * the class of obj.
		 * </p><p>
		 * Use it like this: {@code 
		 * 		return new DeXTable(map, sel.tagFor(Class));
		 * }
		 * </p>
		 */
		public DeXTable serialize(T obj, Serialization sel);
		
		/**
		 * Override this to deserialze a given {@link DeXTable}.
		 */
		public T deserialize(DeXTable table, Serialization sel);
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
		public DeXTable serialize(T obj, Serialization map) {
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
		public T deserialize(DeXTable table, Serialization sel) {
			try {
				T obj = clazz.newInstance();
				for (Field f : fields) {
					if (!f.isAccessible()) f.setAccessible(true);
					f.set(obj, DeX.compose(f.getType(), table.get(f.getName()), sel));
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
						if (Arrays.equals(m.getParameters(), new Class[] { clazz, Serialization.class })) serialize = m;
						else throw new RuntimeException("Couldn't create serializer for class " + clazz + ", wrong argument types!");
					} else throw new RuntimeException("Couldn't create serializer for class " + clazz + ", method is not public!");
				} else if (m.getAnnotation(DeXDeserializer.class) != null) {
					if (deserialize != null) throw new RuntimeException("Second deserializer found in the same class!");
					if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
						if (Arrays.equals(m.getParameters(), new Class[] { DeXTable.class, Serialization.class })) serialize = m;
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
		public DeXTable serialize(T obj, Serialization sel) {
			if (serialize != null) {
				try {
					return (DeXTable) serialize.invoke(obj, sel);
				} catch (Exception e) {
					throw new RuntimeException("Error while trying to serialize Object: ", e);
				}
			} else return super.serialize(obj, sel);
		}

		@Override
		public T deserialize(DeXTable table, Serialization sel) {
			if (deserialize != null) {
				try {
					return (T) deserialize.invoke(null, table, sel);
				} catch (Exception e) {
					throw new RuntimeException("Error while trying to serialize Object: ", e);
				}
			} else return super.deserialize(table, sel);
		}
	}
}
