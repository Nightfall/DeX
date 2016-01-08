package moe.nightfall.dex;

import java.io.File;

public class DeXParser {
	
	private File source;
	
	private DeXParser() {}
	
	public static DeXParser create() {
		return new DeXParser();
	}
	
	public DeXParser source(File file) {
		this.source = file;
		return this;
	}
	
	public DeXParser serialize(String tag, Class<?> serializedClass) {
		return this;
	}
	
	public DeXTable parse() {
		return null;
	}

	public File getSource() {
		return source;
	}
}
