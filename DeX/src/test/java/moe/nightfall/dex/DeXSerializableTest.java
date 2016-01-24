package moe.nightfall.dex;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import moe.nightfall.dex.serialize.DeXSerializable;

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
		
		DeXTable serialized = parser.decompose(map);
		System.out.println("Result: " + serialized);
		assertThat(serialized.equals(expected)).isTrue();
	}
	
	@Test
	public void testDefaultSerialization() {
		DeXTable serialized = parser.decompose(new Point(1, 2));
		Point ret = serialized.compose(Point.class, parser.getSerialization());
		
		assertThat(ret).isEqualTo(new Point(1, 2));
	}
	
	@Test
	public void testAutomaicSerialization() {
		parser.serializeTagAs("point", Point.class);
		DeXTable table = parser.decompose(new Point(100, 100));
		
		assertThat(table.equals(DeXTable.builder("point").put("x", 100).put("y", 100).create())).isTrue();
		
		String output = table.toString();
		DeXTable table2 = parser.parse(output);
		
		assertThat(table2.get(0)).isInstanceOf(Point.class);
	}
	
	static class SelTest implements DeXSerializable {
		@Serialize int x = 10;
		@Serialize List<String> list = Arrays.asList("this", "is", "a", "test");
		
		int y = 100;
	
		SelTest() {}
		SelTest(int x, int y) {
			this.x = x; this.y = y;
		}
	}
	
	@Test
	public void testCustomSerialization() {
		
		SelTest test = new SelTest(10, 20);
		
		DeXTable table = parser.decompose(test);
		
		assertThat(table.equals(
			DeXTable.builder()
				.put("x", 10)
				.put("list", DeXTable.create("this", "is", "a", "test"))
				.create()
			)
		).isTrue();
		
		SelTest second = parser.compose(SelTest.class, table);
		
		assertThat(second.x).isEqualTo(10);
		assertThat(second.y).isEqualTo(100);
	}
}
