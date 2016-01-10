package moe.nightfall.dex;
import org.junit.Test;

public class DeXParserTest {
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
		
		DeXParser parser = DeXParser.create();
		Timer timer = new Timer();
		DeXTable table = parser.parse(source);
		timer.diff();
		System.out.println("Result: " + table.toString());
	}
	
	private static class Timer {
		long millis = System.currentTimeMillis();
		
		void diff() {
			System.out.println("[Took " + (System.currentTimeMillis() - millis) / 1000.0 + " seconds to parse]");
		}
	}
}
