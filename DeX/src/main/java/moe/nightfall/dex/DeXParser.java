package moe.nightfall.dex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import moe.nightfall.dex.DeXSerializable.SerializationMap;

public class DeXParser {
	
	private SerializationMap serialization = new SerializationMap();
	
	private DeXParser() {}
	
	public static DeXParser create() {
		return new DeXParser();
	}
	
	public DeXTable parse(File file) {
		try {
			return parse(String.join("\n", Files.readAllLines(Paths.get(file.toURI()))));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public DeXTable parse(String text) {
		return null;
	}
	
	public DeXTable source(CharSequence cs) {
		return parse(cs.toString());
	}
	
	public DeXParser setSerializationMap(SerializationMap map) {
		this.serialization = map;
		return this;
	}
	
	public DeXParser serializeAs(String tag, Class<?> serializedClass) {
		serialization.put(tag, serializedClass);
		return this;
	}
	
	public DeXTable serialize(Object o) {
		return (DeXTable) DeX.toDeX(o, serialization);
	}
}
