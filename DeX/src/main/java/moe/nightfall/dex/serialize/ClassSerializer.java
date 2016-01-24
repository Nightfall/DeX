package moe.nightfall.dex.serialize;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import moe.nightfall.dex.DeX;
import moe.nightfall.dex.DeXTable;
import moe.nightfall.dex.serialize.DeXSerializable.Serializer;

// TODO Use java 8 lambdas for faster execution speed?
public class ClassSerializer<T> implements Serializer<T> {
	
	protected List<FieldCache> fields = new LinkedList<>();
	
	/**
	 * Field cache, used instead of reflection
	 */
	protected class FieldCache {
		final String name;
		final MethodHandle getter;
		final MethodHandle setter;
		final Class<?> type;
		
		FieldCache(String name, Class<?> type, MethodHandle getter, MethodHandle setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
			this.type = type;
		}
	}
	
	protected final Class<T> clazz;
	protected MethodHandle ctr;
	
	ClassSerializer(Class<T> clazz) {
		Lookup lookup = MethodHandles.lookup();
		
		this.clazz = clazz;
		try {
			Constructor<T> ctr = clazz.getDeclaredConstructor();
			ctr.setAccessible(true);
			this.ctr = lookup.unreflectConstructor(ctr);
		} catch (Exception e) {
			throw new RuntimeException("Can't serialize class " + clazz + ", no empty constructor found.");
		}
		
		try {
			List<Field> reflectedFields = genFields();
			for (Field f : reflectedFields) {
				MethodHandle getter = lookup.unreflectGetter(f).asType(MethodType.methodType(Object.class, Object.class));
				MethodHandle setter = lookup.unreflectSetter(f).asType(MethodType.methodType(void.class, Object.class, Object.class));
				fields.add(new FieldCache(f.getName(), f.getType(), getter, setter));
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Can't serialize class " + clazz, e);
		}
	}
	
	protected List<Field> genFields() {
		
		List<Field> fields = new LinkedList<>();
		Class<?> clazz = this.clazz;
		while (clazz != Object.class) {
			fields.addAll(Arrays.stream(clazz.getDeclaredFields())
				.filter(field -> {
					field.setAccessible(true);
					int modifiers = field.getModifiers();
					return !(Modifier.isStatic(modifiers)
					|| Modifier.isTransient(modifiers));
				}).collect(Collectors.toList()));
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	@Override
	public DeXTable serialize(T obj, Serialization map) {
		DeXTable.Builder builder = DeXTable.builder(map.tagFor(clazz), fields.size());
		for (FieldCache field : fields) {
			try {
				builder.put(field.name, DeX.decompose(field.getter.invokeExact(obj), map));
			} catch (Throwable t) {
				throw new RuntimeException("Error while trying to serialize Object: ", t);
			}
		}
		return builder.create();
	}

	@Override
	public T deserialize(DeXTable table, Serialization sel) {
		try {
			T obj = (T) ctr.invoke();
			for (FieldCache field : fields) {
				field.setter.invokeExact(obj, DeX.compose(field.type, table.get(field.name), sel));
			}
			return obj;
		} catch (Throwable t) {
			throw new RuntimeException("Error while trying to deserialize from table: ", t);
		}
	}
}