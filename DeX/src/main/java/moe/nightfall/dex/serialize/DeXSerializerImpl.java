package moe.nightfall.dex.serialize;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import moe.nightfall.dex.DeXTable;
import moe.nightfall.dex.serialize.DeXSerializable.DeXDeserializer;
import moe.nightfall.dex.serialize.DeXSerializable.DeXSerializer;
import moe.nightfall.dex.serialize.DeXSerializable.Serialize;

class DeXSerializerImpl<T extends DeXSerializable> extends ClassSerializer<T> {
	
	private MethodHandle serialize;
	private MethodHandle deserialize;
	
	DeXSerializerImpl(Class<T> clazz) {
		super(clazz);
		
		Lookup lookup = MethodHandles.lookup();
		
		try {
			for (Method m : clazz.getDeclaredMethods()) {
				int modifiers = m.getModifiers();
				if (m.getAnnotation(DeXSerializer.class) != null) {
					if (serialize != null) throw new RuntimeException("Second serializer found in the same class!");
					if (Arrays.equals(m.getParameterTypes(), new Class[] { Serialization.class }) && m.getReturnType() == DeXTable.class) {
						m.setAccessible(true);
						serialize = lookup.unreflect(m).asType(MethodType.methodType(DeXTable.class, DeXSerializable.class, Serialization.class));
					} else throw new RuntimeException("Couldn't create serializer for class " + clazz + ", wrong argument types!");
				} else if (m.getAnnotation(DeXDeserializer.class) != null) {
					if (deserialize != null) throw new RuntimeException("Second deserializer found in the same class!");
					if (Modifier.isStatic(modifiers)) {
						if (Arrays.equals(m.getParameterTypes(), new Class[] { DeXTable.class, Serialization.class }) && clazz.isAssignableFrom(m.getReturnType())) {
							m.setAccessible(true);
							deserialize = lookup.unreflect(m).asType(MethodType.methodType(DeXSerializable.class, DeXTable.class, Serialization.class));
						} else throw new RuntimeException("Couldn't create deserializer for class " + clazz + ", wrong argument types!");
					} else throw new RuntimeException("Couldn't create deserialzer for class " + clazz + ", method isn't static!");
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override 
	protected List<Field> genFields() {
		return super.genFields().stream().filter(field -> field.getAnnotation(Serialize.class) != null).collect(Collectors.toList());
	}

	@Override
	public DeXTable serialize(T obj, Serialization sel) {
		if (serialize != null) {
			try {
				return (DeXTable) serialize.invokeExact(obj, sel);
			} catch (Throwable t) {
				throw new RuntimeException("Error while trying to serialize Object: ", t);
			}
		} else return super.serialize(obj, sel);
	}

	@Override
	public T deserialize(DeXTable table, Serialization sel) {
		if (deserialize != null) {
			try {
				return (T) deserialize.invokeExact(table, sel);
			} catch (Throwable t) {
				throw new RuntimeException("Error while trying to serialize Object: ", t);
			}
		} else return super.deserialize(table, sel);
	}
}