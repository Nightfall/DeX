package moe.nightfall.dex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import moe.nightfall.dex.DeXSerializable.SerializationMap;

public class DeXParser {
	
	private SerializationMap serialization = new SerializationMap();
	private boolean skipKeyValidation = false;
	
	private DeXParser() {}
	
	public static DeXParser create() {
		return new DeXParser();
	}
	
	/**
	 * This skips validation of duplicate keys, makes
	 * parsing slightly faster at the cost of parsing
	 * files that do not comply with the standard.
	 */
	public DeXParser skipKeyValidation() {
		skipKeyValidation = true;
		return this;
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
	
	/** Mutable table for parsing */
	private final class RawTable {

		String tag = "";
		boolean array = true;
		Set<Entry> values = new HashSet<>();
		
		// Contains all tagged tables with automatic conversion
		List<RawTable> tagged = new LinkedList<>();
		
		String source;
		int index, line;
		
		DeXTable compile() {
			DeXTable table = new DeXTable(values.size(), tag);
			
			// Sanity check, tell if any autokeyed tagged tables have duplicate keys 
			if (!array && !DeXParser.this.skipKeyValidation) {
				for (RawTable raw : tagged) {
					for (Entry entry : values) {
						if (entry.key.equals(raw.tag)) throw new KeyDuplicationError(raw.index, raw.line, raw.source);
					}
				}
			}
			
			for (Entry entry : values) {
				Object value = entry.value;
				if (value instanceof RawTable) value = ((RawTable) value).compile();
				Object key = entry.key;
				if (key instanceof RawTable) value = ((RawTable) key).compile();
				table.append(key, value);
			}
			
			return table;
		}
		
		void add(Object key, Object value, int index, int line, String source) {
			// This is not an array.
			if (key != null) array = false;
			
			boolean skipCheck = false;
			if (key == null) {
				if (value instanceof RawTable) {
					RawTable table = (RawTable) value;
					String tag = table.tag;
					if (tag.length() > 0) {
						if (!DeXParser.this.skipKeyValidation) {
							// Cache this for later, we can't do the check for them just now since
							// We don't know if this is an array or not yet
							tagged.add(table);
							table.index = index;
							table.line = line;
							table.source = source;
						}
						key = tag;
						skipCheck = true;
					}
				}
			}
			
			if (!skipCheck && !DeXParser.this.skipKeyValidation && key != null) {
				for (Entry entry : values) {
					if (entry.key.equals(key)) throw new KeyDuplicationError(index, line, source);
				}
			}
			
			values.add(new Entry(key, value));
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(tag, values);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (obj instanceof RawTable) {
				RawTable other = (RawTable) obj;
				return other.tag.equals(tag) && other.values.equals(values);
			}
			return false;
		}
	}
	
	private static final class Entry {
		Object key, value;
		
		Entry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (obj instanceof Entry) {
				return key.equals(((Entry) obj).key);
			}
			return false;
		}
	}
	
	
	public DeXTable parse(String text) {
		return null;
	}
	
	public DeXTable parse(CharSequence cs) {
		return parse(cs.toString());
	}
	
	public DeXTable parse(File file) {
		try {
			return parse(String.join("\n", Files.readAllLines(Paths.get(file.toURI()))));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public class ParseException extends RuntimeException {
		private static final long serialVersionUID = 2169899005064859506L;
		
		ParseException(int index, int line, String source, String message) {
			super(message + ":\n" + generateDetailMessage(index, line, source));
		}
	}
	
	public class KeyDuplicationError extends ParseException {
		KeyDuplicationError(int index, int line, String source) {
			super(index, line, source, "Duplicate key found");
		}
	}
	
	/**
	 * Generates a string with the two lines before and two lines after an error,
	 * pointing at the error
	 */
	private static String generateDetailMessage(int index, int line, String source) {
		
		StringBuilder ret = new StringBuilder();
		
		int lineStart = source.lastIndexOf('\n', index);
		int lineEnd = source.indexOf('\n', index);
		
		final int range = 2;
		String[] lines = new String[range * 2 + 1]; 
		
		if (lineStart == -1) lineStart = 0;
		else {
			int ls2 = lineStart;
			for (int i = 0; i < range; i++) {
				int ls3 = source.lastIndexOf('\n', ls2);
				if (ls3 == -1) break;
				lines[range - i] = source.substring(ls3, ls2);
				ls2 = ls3;
			}
		}
		
		if (lineEnd == -1) lineEnd = source.length();
		else {
			int ls2 = lineEnd;
			for (int i = 0; i < range; i++) {
				int ls3 = source.indexOf('\n', ls2);
				if (ls3 == -1) break;
				lines[range + 1 + i] = source.substring(ls2, ls3);
				ls2 = ls3;
			}
		}
	
		lines[range + 1] = source.substring(lineStart, index) + " >>> " + source.substring(index + 1, lineEnd);
		
		for (int i = 0; i < lines.length; i++) {
			String l = lines[i];
			if (l == null) continue;
			int lineNumber = line - range + i;
			
			if (i == range + 1) {
				ret.append('\n');
			}
			
			ret.append(lineNumber).append(": ");
			ret.append(l);
			
			if (i != lines.length - 1) {
				ret.append('\n');
			}
			if (i == range + 1) {
				ret.append('\n');
			}
		}
		
		return ret.toString();
	}
}
