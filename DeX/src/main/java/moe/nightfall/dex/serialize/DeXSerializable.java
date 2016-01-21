package moe.nightfall.dex.serialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import moe.nightfall.dex.DeXTable;

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
}
