package moe.nightfall.dex;

import org.junit.Before;
import org.junit.Test;

public class DeXJSONTest {
	
	DeXParser parser;
	
	@Before
	public void init() {
		parser = DeXParser.create();
	}
	
	@Test
	public void testJSONConversion() {
		
		String json = "{\"menu\": {\"id\": \"file\",\"value\": \"File\",\"popup\": {\"menuitem\": [{\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"},{\"value\": \"Open\", \"onclick\": \"OpenDoc()\"},{\"value\": \"Close\", \"onclick\": \"CloseDoc()\"}]}}}";
	
		DeXTable result = parser.parseJSON(true).parse(json);
		
		System.out.println("Result: " + result);
	}
}
