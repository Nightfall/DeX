package moe.nightfall.dex.serialize;

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

// TODO Here we can use the metafactory
public class DeXSerializerImpl<T extends DeXSerializable> extends ClassSerializer<T> {
	
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
	protected List<Field> genFields() {
		return super.genFields().stream().filter(field -> field.getAnnotation(Serialize.class) != null).collect(Collectors.toList());
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