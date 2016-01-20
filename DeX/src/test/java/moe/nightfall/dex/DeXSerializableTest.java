package moe.nightfall.dex;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class DeXSerializableTest {
	
	DeXParser parser;
	
	@Before
	public void init() {
		parser = DeXParser.create();
	}
	
	@Test
	public void testNativeSerialization() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("double", 1.234D);
		map.put("int", 1234);
		map.put("string", "test");
		map.put("array", new Object[]{1, 2, "abc", new Object[]{1, 2, 3}, "def"});
		map.put("list", Arrays.asList(1, 2, "abc", 4, "def"));
		
		DeXTable expected = DeXTable.builder()
			.put("double", 1.234D)
			.put("int", 1234)
			.put("string", "test")
			.put("array", DeXTable.create(1, 2, "abc", DeXTable.create(1, 2, 3), "def"))
			.put("list", DeXTable.create(1, 2, "abc", 4, "def"))
			.create();
		
		DeXTable serialized = parser.serialize(map);
		System.out.println("Result: " + serialized);
		assertThat(parser.serialize(map).equals(expected));
	}
	
	@Test
	public void testDefaultSerialization() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("coordinate", new Point(1, 2));
		
		DeXTable serialized = parser.serialize(map);
		System.out.println("Result: " + serialized);
	}
}
