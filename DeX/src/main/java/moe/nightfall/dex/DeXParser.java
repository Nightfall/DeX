package moe.nightfall.dex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import moe.nightfall.dex.DeXSerializable.SerializationMap;

public class DeXParser {
	
	private String source;
	private SerializationMap serialization = new SerializationMap();
	
	private DeXParser() {}
	
	public static DeXParser create() {
		return new DeXParser();
	}
	
	public DeXParser source(File file) {
		try {
			this.source = String.join("\n", Files.readAllLines(Paths.get(file.toURI())));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	public DeXParser source(String text) {
		this.source = text;
		return this;
	}
	
	public DeXParser source(CharSequence cs) {
		this.source = cs.toString();
		return this;
	}
	
	public DeXParser setSerializationMap(SerializationMap map) {
		this.serialization = map;
		return this;
	}
	
	public DeXParser serialize(String tag, Class<?> serializedClass) {
		serialization.put(tag, serializedClass);
		return this;
	}
	
	public DeXTable parse() {
		source = null;
		return null;
	}
	
	public DeXTable serialize(Object o) {
		return (DeXTable) DeX.toDeX(o, serialization);
	}
}
