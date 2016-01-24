package moe.nightfall.dex.serialize;

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
	
	protected List<Field> fields;
	protected final Class<T> clazz;
	protected Constructor<T> ctr;
	
	ClassSerializer(Class<T> clazz) {
		this.clazz = clazz;
		try {
			this.ctr = clazz.getDeclaredConstructor();
			ctr.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException("Can't serialize class " + clazz + ", no empty constructor found.");
		}
		fields = genFields();
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
			T obj = ctr.newInstance();
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