package moe.nightfall.dex.serialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import moe.nightfall.dex.DeXParser;
import moe.nightfall.dex.DeXTable;

/**
 * This is a marker interface to be implemented by any class that handles
 * its own serialization, using annotations. An alternative to using this interface
 * (for example if you don't have access to the underlying type) is using
 * {@link Serialization#serializeAs(Class, Serializer)} or {@link DeXParser#serializeAs(Class, Serializer)} respectively.
 * 
 * @see DeXSerializer
 * @see DeXDeserializer
 * @see Serialize
 */
public interface DeXSerializable {
	
	/**
	 * Use this annotation to mark a method with the signature of
	 * <p>{@code [VISIBILITY] static <? super TYPE> NAME (DeXTable, Serialization)}</p>
	 * as deserializer for the wrapping class. If you need to access the default
	 * implementation, use {@link ClassSerializer#deserialize(ClassSerializer, DeXTable, Serialization)}
	 * like this:
	 * <p>{@code DeXTable table = ClassSerializer.deserialize((ClassSerializer<TYPE>) sel.forClass(TYPE.class), table, sel)}</p>
	 * 
	 * @see DeXSerializer
	 * @see Serialize
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface DeXDeserializer {}
	
	/**
	 * Use this annotation to mark a method with the signature of
	 * <p>{@code [VISIBILITY] DeXTable NAME (Serialization)}</p>
	 * as serializer for the wrapping class. If you need to acces the default
	 * implementation, use {@link ClassSerializer#serialize(ClassSerializer, Object, Serialization)}
	 * like this:
	 * <p>{@code TYPE obj = ClassSerializer.serialize((ClassSerializer<TYPE>) sel.forClass(TYPE.class), this, sel)}</p>
	 * 
	 * @see DeXDeserializer
	 * @see Serialize
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface DeXSerializer {}
	
	/**
	 * Use this annotation to mark a field to be serialized
	 * by the default implementation of {@link Serializer}
	 * 
	 * @see DeXSerializer
	 * @see DeXDeserializer
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface Serialize {}
	
	public static interface Serializer<T> {
		
		/** 
		 * Override this to serialize a given Object.
		 * By convention the returned table has the tag
		 * defined in the given {@link Serialization} for
		 * the class of obj.
		 */
		public DeXTable serialize(T obj, Serialization sel);
		
		/**
		 * Override this to deserialze a given {@link DeXTable}.
		 */
		public T deserialize(DeXTable table, Serialization sel);
	}
}
