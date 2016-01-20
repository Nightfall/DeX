package moe.nightfall.dex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;

import moe.nightfall.dex.DeXParser.ParseException;
import moe.nightfall.dex.DeXParser.UnexpectedTokenException;

public class DeXParserTest {
	
	DeXParser parser;
	
	@Before
	public void init() {
		parser = DeXParser.create();
	}
	
	@Test
	public void testComplexTable() {
		String source = 
			"MAYU : vocaloid {\n" +
			"taglist { yandere, lolita, gothic, small }\n" +

  			"gender   : female\n" +
			"age      : 15\n" +
			"company  : EXIT TUNES\n" +
			"language : Japanese\n" +
			"code     : QWCE-00264\n" +

  			"description: \"\n" +
			"  MAYU's design is based on gothic lolita fashion.\n" +
			"  Her hair itself fades from a light blonde to rainbow,\n" +
			"  and she is depicted with a hat that has a speaker attached.\n" +
			"  Her earrings appear to be styled like in-ear headphones that hook over the ear.\n" +
			"\"\n" +
			"}";
		
		Timer timer = new Timer();
		DeXTable table = parser.parse(source);
		timer.diff();

		System.out.println("Result: " + table);
		System.out.println("Raw: " + DeX.print(table));
		
		DeXTable expected = DeXTable.builder()
			.put("MAYU", DeXTable.builder("vocaloid")
				.put("taglist", DeXTable.builder("taglist").addAll("yandere", "lolita", "gothic", "small").create())
				
				.put("gender", "female")
				.put("age", 15)
				.put("company", "EXIT TUNES")
				.put("language", "Japanese")
				.put("code", "QWCE-00264")
				
				.put("description", "\n" +
					"  MAYU's design is based on gothic lolita fashion.\n" +
					"  Her hair itself fades from a light blonde to rainbow,\n" +
					"  and she is depicted with a hat that has a speaker attached.\n" +
					"  Her earrings appear to be styled like in-ear headphones that hook over the ear.\n"
				).create()
			).create();
		
		assertThat(table.equals(expected)).isTrue();
	}
	
	@Test
	public void testArrays() {
		
		DeXTable table;
		// Empty array
		table = parser.parse("{}").getTable(0);

		assertThat(table.isArray()).isTrue();
		assertThat(table.size() == 0).isTrue();
		
		// Simple array
		table = parser.parse("{one, foo bar, three}").getTable(0);
		assertThat(table.isArray()).isTrue();
		assertThat(table.equals(DeXTable.create("one", "foo bar", "three"))).isTrue();
	}
	
	@Test
	public void testFlags() {
		assertThat(parser.parse("{+crazy, -sane}").getTable(0).equals(DeXTable.builder().put("crazy", true).put("sane", false).create())).isTrue();
		
	}
	
	@Test
	public void testEmptyValue() {
		assertThatThrownBy(() -> parser.parse("{val1, val2,,}")).isInstanceOf(UnexpectedTokenException.class);
		assertThatThrownBy(() -> parser.parse("{,val1, val2} ")).isInstanceOf(UnexpectedTokenException.class);
	}

	@Test
	public void testInvalidString() {
		assertThatThrownBy(() -> parser.parse("{this is \" invalid!}")).isInstanceOf(UnexpectedTokenException.class);
		assertThatThrownBy(() -> parser.parse("{\"this is invalid!} ")).isInstanceOf(UnexpectedTokenException.class);
	}
	
	@Test
	public void testInvalidTable() {
		assertThatThrownBy(() -> parser.parse("{")).isInstanceOf(ParseException.class);
		assertThatThrownBy(() -> parser.parse("}")).isInstanceOf(ParseException.class);
		assertThatThrownBy(() -> parser.parse("{a, b}, c}")).isInstanceOf(ParseException.class);
		
		assertThatThrownBy(() -> parser.parse("{value}{value}")).isInstanceOf(ParseException.class);
	}
	
	private static class Timer {
		long millis = System.currentTimeMillis();
		
		void diff() {
			System.out.println("[Took " + (System.currentTimeMillis() - millis) / 1000.0 + " seconds to parse]");
		}
	}
}
