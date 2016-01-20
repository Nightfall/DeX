package moe.nightfall.dex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EmptyStackException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import moe.nightfall.dex.DeXSerializable.SerializationMap;

public class DeXParser {
	
	private SerializationMap serialization = new SerializationMap();
	
	private DeXParser() {}
	
	public static DeXParser create() {
		return new DeXParser();
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
		Set<Entry> values = new LinkedHashSet<>();

		RawTable(String tag) {
			this.tag = tag;
		}

		DeXTable compile() {
			DeXTable.Builder builder = DeXTable.builder(tag, values.size());
			int i = 0;
			for (Entry entry : values) {
				Object value = entry.value;
				Object key = i++;
				if (!array) {
					if (entry.key != null) key = entry.key;
				}
				builder.put(key, value);
			}
			
			return builder.create();
		}
		
		void add(Object key, Object value, int index, int line, String source) {
			
			// Filter out empty
			if (value == null) return;
			// This is not an array.
			if (key != null) array = false;
			
			if (key == null) {
				if (value instanceof DeXTable) {
					DeXTable table = (DeXTable) value;
					if (table.hasTag()) {
						key = table.tag();
					}
				}
			}
			values.add(new Entry(key, value));
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
		
		// True if we are currently in string context, means " was opened
		boolean stringContext = false;
		// True if we WERE in a string context this token
		boolean wasStringContext = false;
		
		// This is where we are at now;
		int lineNumber = 0;
		int index = 0;
		
		boolean escape = false;
		
		public ParserData(String source) {
			this.source = source;
			this.stack.add(new Entry(null, baseTable));
		}
		
		void push(char c) {
			Entry entry = stack.peek();
			if (entry.value instanceof StringBuilder) {
				StringBuilder sb = ((StringBuilder) entry.value);
				sb.append(c);
			} else if (entry.value == null) { 
				// A whitespace can't start a new token
				if (c == ' ') return;
				entry.value = new StringBuilder().append(c);
			} else {
				// Can't insert, need a new entry
				// A whitespace can't start a new token
				if (c == ' ') return;
				stack.push(new Entry(null, new StringBuilder().append(c)));
			}
		}
		
		void popKey() {
			wasStringContext = false;
			Entry entry = stack.peek();
			if (entry.value == null) {
				throw new UnexpectedTokenException(index, lineNumber, source);
			} else {
				entry.key = entry.value;
				if (entry.key instanceof StringBuilder)
					entry.key = coerce((StringBuilder) entry.key);
				entry.value = null;
				//System.out.println("Popped key " + entry.key);
			}
		}
		
		void pushTable() {
//			System.out.println("Pushed table");
			Entry entry = stack.peek();
			if (entry.value instanceof StringBuilder) {
				wasStringContext = false;
				if (stringContext) throw new UnexpectedTokenException(index, lineNumber, source, "\" expected");
				entry.value = new RawTable(stripTrailingWhitespace(entry.value.toString()));
			} else if (entry.value == null) {
				entry.value = new RawTable("");
			} else { 
				// Can't insert, need a new entry
				stack.push(new Entry(null, new RawTable("")));
			}
		}
		
		void popTable() {
			try {
				Entry entry = stack.peek();
				if (!(entry.value instanceof RawTable)) {
					// Remove last index on the list S
					popValue(false);
					entry = stack.peek();
				}
				
				if (!(entry.value instanceof RawTable) || stack.size() < 2) 
					throw new UnexpectedTokenException(index, lineNumber, source);
				entry.value = ((RawTable)entry.value).compile();
			} catch (EmptyStackException e) {
				throw e;
			}
		}
		
		void popValue(boolean eol) {
			wasStringContext = false;
			if (stringContext) throw new UnexpectedTokenException(index, lineNumber, source, "\" expected");
			try {
				Entry entry = stack.peek();
				if (entry.value instanceof RawTable) {
					if (eol) return;
					else throw new UnexpectedTokenException(index, lineNumber, source);
				}
				
				stack.pop();
				
				//System.out.println("Pop: s:" + stack.size() + " " + entry.key + " " + entry.value);
				Entry parent = stack.peek();
				if (parent.value instanceof RawTable) {
					if (entry.value == null) throw new UnexpectedTokenException(index, lineNumber, source);
					
					RawTable table = (RawTable) parent.value;
					if (entry.value instanceof StringBuilder)
						entry.value = coerce((StringBuilder) entry.value);
					table.add(entry.key, entry.value, index, lineNumber, source);
				} else {
					// TODO Error out
					throw new UnexpectedTokenException(index, lineNumber, source);
				}
			} catch (EmptyStackException e) {
				throw e;
			}
		}
		
		Object coerce(StringBuilder value) {
			String s = value.toString();
			s = stripTrailingWhitespace(s);
			
			if (!stringContext) {
				Double number = DeX.parseDeXNumber(s);
				if (number != null) return number;
			}
			return s;
		}
		
		private String stripTrailingWhitespace(String s) {
			if (wasStringContext) return s;
			// Get rid of all trailing whitespace
			char c = s.charAt(s.length() - 1);
			if (c == ' ') {
				int index = s.length() - 1;
				while (true) {
					index--;
					if (s.charAt(index) != ' ') {
						s = s.substring(0, index + 1);
						break;
					}
				}
			}
			return s;
		}
	}
	
	public DeXTable parse(String text) {
		ParserData d = new ParserData(text);
		try {
			for (d.index = 0; d.index < text.length(); d.index++) {
				char c = text.charAt(d.index);
				
				if (c == '"' && !d.escape) {
					if (d.stringContext) {
						d.stringContext = false;
					} else {
						if (d.wasStringContext) throw new UnexpectedTokenException(d.index, d.lineNumber, d.source);
						d.stringContext = true;
						d.wasStringContext = true;
						
						// Check if this is the first token, otherwise fail
						Entry current = d.stack.peek();
						if (current.value != null) throw new UnexpectedTokenException(d.index, d.lineNumber, d.source);
					}
				} else if (c  == '\t' && d.stringContext) {
					// Don't parse tab, unless in string context
					d.push(c);
				} else if (c == '\\') {
					if (d.escape && d.stringContext) {
						d.push(c);
						d.escape = false;
					} else {
						d.escape = true;
					}
				} else if (c == '\n') {
					d.lineNumber++;
					if (d.stringContext) d.push(c);
					else {
						if (!d.escape) {
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
			throw new ParseException(d.index, d.lineNumber, d.source, "Unexpected end of file", e);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(d.index, d.lineNumber, d.source, "Unexpected error uccorded!", e);
		}
		
		// Check if anything is left on the stack
		if (d.stack.size() > 1) d.popValue(true);
		if (d.stack.size() != 1) throw new ParseException(d.index, d.lineNumber, d.source, "Unexpected end of file");
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
	//TODO This fails once in a while so check what's up with this...
	private static String generateDetailMessage(int index, int line, String source) {
		if (source == null) return "?";
		
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
					if (ls2 < source.length() - 1) ls3 = source.length() - 1;
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
