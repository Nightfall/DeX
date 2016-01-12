package moe.nightfall.dex;
import org.junit.Assert;
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
		System.out.println("Result: " + table.toString());
	}
	
	@Test
	public void testArrays() {
		
		DeXTable table;
		// Empty array
		table = parser.parse("{}");
		Assert.assertTrue(table.isArray());
		
		// Simple array
		table = parser.parse("{one, foo bar, three}");
		Assert.assertTrue(table.isArray());
	}
	
	@Test(expected = UnexpectedTokenException.class)
	public void testEmptyValue2() {
		parser.parse("{val1, val2,,}");
	}
	
	@Test(expected = UnexpectedTokenException.class)
	public void testInvalidString1() {
		parser.parse("{this is \" invalid!}");
	}
	
	@Test(expected = UnexpectedTokenException.class)
	public void testInvalidString2() {
		parser.parse("{\"this is invalid!}");
	}
	
	@Test(expected = ParseException.class)
	public void testInvalidTable1() {
		parser.parse("{");
	}
	
	@Test(expected = ParseException.class)
	public void testInvalidTable2() {
		parser.parse("{value} {value}");
	}
	
	private static class Timer {
		long millis = System.currentTimeMillis();
		
		void diff() {
			System.out.println("[Took " + (System.currentTimeMillis() - millis) / 1000.0 + " seconds to parse]");
		}
	}
}
