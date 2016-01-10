package moe.nightfall.dex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

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

		final String tag;
		boolean array = true;
		Set<Entry> values = new HashSet<>();
		
		// Contains all tagged tables with automatic conversion
		List<RawTable> tagged = new LinkedList<>();
		
		String source;
		int index, line;
		
		RawTable(String tag) {
			this.tag = tag;
		}

		DeXTable compile() {
			DeXTable table = new DeXTable(values.size(), tag);
			
			// Sanity check, tell if any autokeyed tagged tables have duplicate keys 
			if (!array && !DeXParser.this.skipKeyValidation) {
				int count = 0;
				for (RawTable raw : tagged) {
					for (Entry entry : values) {
						if (entry.key.equals(raw.tag)) count++;
						if (count > 1) throw new KeyDuplicationException(raw.index, raw.line, raw.source);
					}
				}
			}
			
			int i = 0;
			for (Entry entry : values) {
				Object value = entry.value;
				if (value instanceof RawTable) value = ((RawTable) value).compile();
				
				Object key = i++;
				if (!array) {
					key = entry.key;
					if (key instanceof RawTable) value = ((RawTable) key).compile();
				}
				
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
			
			if (!skipCheck && key != null && !DeXParser.this.skipKeyValidation ) {
				for (Entry entry : values) {
					if (entry.key.equals(key)) throw new KeyDuplicationException(index, line, source);
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
			if (key != null)
				return key.hashCode();
			else return super.hashCode();
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
	
	private final class ParserData {
		
		final String source;

		RawTable baseTable = new RawTable("");
		Stack<Entry> stack = new Stack<>();
		boolean stringContext = false;
		
		// This is where we are at now;
		int lineNumber = 0;
		int index = 0;
		
		boolean escape = false;
		
		public ParserData(String source) {
			this.source = source;
			this.stack.add(new Entry(null, baseTable));
		}
		
		void push(char c) {
			Entry current = stack.peek();
			if (current.value == null) {
				current.value = new StringBuilder().append(c);
			} else if (current.value instanceof StringBuilder) {
				((StringBuilder) current.value).append(c);
			} else if (current.key != null || current.value instanceof RawTable) {
				// Can't insert, need a new entry
				stack.push(new Entry(null, new StringBuilder().append(c)));
			} else {
				throw new UnexpectedTokenException(index, lineNumber, source);
			}
		}
		
		void popKey() {
			Entry entry = stack.peek();
			if (entry.value == null) {
				throw new UnexpectedTokenException(index, lineNumber, source);
			} else {
				entry.key = entry.value;
				if (entry.key instanceof StringBuilder)
					entry.key = coerce((StringBuilder) entry.key);
				entry.value = null;
			}
		}
		
		void pushTable() {
			Entry entry = stack.peek();
			if (entry.value instanceof StringBuilder) {
				entry.value = new RawTable(entry.value.toString());
			} else if (entry.value == null) {
				entry.value = new RawTable("");
			} else if (entry.key != null){
				// Can't insert, need a new entry
				stack.push(new Entry(null, new RawTable("")));
			} else { 
				// TODO Error Out
				throw new UnexpectedTokenException(index, lineNumber, source);
			}
		}
		
		void popTable() {
			try {
				Entry entry = stack.peek();
				if (!(entry.value instanceof RawTable)) popValue(false);
				entry = stack.pop();
				Entry parent = stack.peek();
				if (parent.value instanceof RawTable && entry.value instanceof RawTable) {
					RawTable table = (RawTable) parent.value;
					table.add(entry.key, entry.value, index, lineNumber, source);
				} else {
					// TODO Error out
					throw new UnexpectedTokenException(index, lineNumber, source);
				}
			} catch (EmptyStackException e) {
				throw e;
			}
		}
		
		void popValue(boolean eol) {
			try {
				Entry entry = stack.peek();
				if (eol) {
					// Don't be as strict if this is a line end, check if you'd
					// pop a table and return if so.
					if (entry.value instanceof RawTable) return;
				}
				
				stack.pop();
				Entry parent = stack.peek();
				if (parent.value instanceof RawTable && !(entry.value instanceof RawTable)) {
					RawTable table = (RawTable) parent.value;
					if (entry.value instanceof StringBuilder)
						entry.value = coerce((StringBuilder) entry.value);
					table.add(entry.key, entry.value, index, lineNumber, source);
				} else if (!eol) {
					// TODO Error out
					throw new UnexpectedTokenException(index, lineNumber, source);
				}
			} catch (EmptyStackException e) {
				throw e;
			}
		}
		
		Object coerce(StringBuilder value) {
			return value;
		}
	}
	
	public DeXTable parse(String text) {
		ParserData d = new ParserData(text);
		try {
			for (d.index = 0; d.index < text.length(); d.index++) {
				char c = text.charAt(d.index);
				
				if (c == '"' && !d.escape) {
					d.stringContext = !d.stringContext;
				} else if (c == ' ' || c  == '\t') {
					// Don't parse whitespace, unless in string context
					if (d.stringContext) {
						d.push(c);
					}
				} else if (c == '\\') {
					if (d.escape && d.stringContext) {
						d.push(c);
						d.escape = false;
					} else {
						d.escape = true;
					}
				} else if (c == '\n') {
					if (d.stringContext) d.push(c);
					else {
						if (!d.escape) {
							d.lineNumber++;
							d.popValue(true);
						} else {
							d.escape = false;
						}
					}
				} else if (d.escape) {
					if (d.stringContext) {
						// Escape sequences
						switch (c) {
						case '\\' : d.push(c); break;
						case 'b' : d.push('\b'); break;
						case 't' : d.push('\t'); break;
						case 'n' : d.push('\n'); break;
						case 'f' : d.push('\f'); break;
						case 'r' : d.push('\r'); break;
						case '"' : d.push('\"'); break;
						case 'u' :
							String sequence = d.source.substring(d.index, d.index + 4);
							try {
								d.push((char) Integer.parseInt(sequence, 16));
							} catch (NumberFormatException e) {
								throw new InvalidEscapeSequenceException(d.index, d.lineNumber, d.source);
							}
							d.index += 4;
							break;
						}
					} else {
						throw new UnexpectedTokenException(d.index, d.lineNumber, d.source, "Found escape sequence outside of string context");
					}
					d.escape = false;
				} else if (!d.stringContext) {
					if (c == ',') {
						d.popValue(false);
					} else if (c == ':') {
						d.popKey();
					} else if (c == '{') {
						d.pushTable();
					} else if (c == '}') {
						d.popTable();
					} else d.push(c);
				} else {
					d.push(c);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException(d.index, d.lineNumber, d.source, "Unexpected end of file");
		} catch (Exception e) {
			throw new ParseException(d.index, d.lineNumber, d.source, "Unexpected error uccorded!", e);
		}
		return d.baseTable.compile();
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
	
	public static class ParseException extends RuntimeException {
		private static final long serialVersionUID = 2169899005064859506L;
		
		ParseException(int index, int line, String source, String message) {
			super(message + ":\n" + generateDetailMessage(index, line, source));
		}
		ParseException(int index, int line, String source, String message, Exception e) {
			super(message + ":\n" + generateDetailMessage(index, line, source), e);
		}
	}
	
	public static class KeyDuplicationException extends ParseException {
		private static final long serialVersionUID = -9210668030112796649L;

		KeyDuplicationException(int index, int line, String source) {
			super(index, line, source, "Duplicate key found");
		}
	}
	
	public static class UnexpectedTokenException extends ParseException {
		private static final long serialVersionUID = -1101771027977345797L;
		
		UnexpectedTokenException(int index, int line, String source) {
			super(index, line, source, "Unexpected token found");
		}
		UnexpectedTokenException(int index, int line, String source, String detail) {
			super(index, line, source, detail);
		}
	}
	
	public static class InvalidEscapeSequenceException extends ParseException {
		private static final long serialVersionUID = -2566118826766886695L;

		InvalidEscapeSequenceException(int index, int line, String source) {
			super(index, line, source, "Invalid escape sequence found");
		}
	}
	
	/**
	 * Generates a string with the two lines before and two lines after an error,
	 * pointing at the error
	 */
	private static String generateDetailMessage(int index, int line, String source) {
		
		StringBuilder ret = new StringBuilder();
		
		int lineStart = source.lastIndexOf('\n', index - 1);
		int lineEnd = source.indexOf('\n', index);
		
		final int range = 2;
		String[] lines = new String[range * 2 + 1]; 
		
		if (lineStart == -1) lineStart = 0;
		else {
			int ls2 = lineStart;
			for (int i = 0; i < range; i++) {
				int ls3 = source.lastIndexOf('\n', ls2 - 1);
				if (ls3 == -1) {
					if (ls2 > 0) ls3 = 0;
					else break;
				}
				lines[range - 1 - i] = source.substring(ls3, ls2);
				ls2 = ls3;
			}
		}
		
		if (lineEnd == -1) lineEnd = source.length();
		else {
			int ls2 = lineEnd;
			for (int i = 0; i < range; i++) {
				int ls3 = source.indexOf('\n', ls2 + 1);
				if (ls3 == -1) {
					if (ls2 < source.length()) ls3 = source.length() - 2;
					else break;
				}
				
				lines[range + 1 + i] = source.substring(ls2 + 1, ls3);
				ls2 = ls3;
			}
		}
	
		lines[range] = source.substring(Math.min(lineStart + 1, index), index) + " >>>" + source.substring(index, lineEnd);
		
		for (int i = 0; i < lines.length; i++) {
			String l = lines[i];
			if (l == null) continue;
			int lineNumber = line - range + i;
			
			ret.append(lineNumber).append(": ");
			ret.append(l);
			
			if (i != lines.length - 1) {
				ret.append('\n');
			}
		}
		
		return ret.toString();
	}
}
