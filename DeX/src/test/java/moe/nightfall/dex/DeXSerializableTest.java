package moe.nightfall.dex;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Button;
import java.awt.Color;
import java.awt.Cursor;
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
		
		DeXTable serialized = parser.decompose(map);
		System.out.println("Result: " + serialized);
		assertThat(serialized.equals(expected));
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
	
	@Test
	public void testComplexSerialization() {
		
		parser.serializeTagAs("button", Button.class);
		
		// Buttons are serializable
		Button button = new Button("testButton");
		button.setSize(120, 120);
		button.setBackground(Color.red);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		button.setVisible(false);
		
		DeXTable table = parser.decompose(button);
		DeXTable output = parser.parse(DeX.print(table));
		
		// Doesn't properly support hasCode / equals so comparing those has to be good enough
		assertThat(output.get(0).toString()).isEqualTo(button.toString());
	}
}
