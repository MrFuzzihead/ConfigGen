package org.squiddev.configgen;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.squiddev.configgen.PrimitiveConfigMetadata.Category;
import static org.squiddev.configgen.PrimitiveConfigMetadata.Property;

public class TestPropertyMetadata {
	@Test
	public void testBasic() {
		getProperty("basic", "foo").set(2);
		getProperty("basic", "bar").set("qux");

		assertEquals(2, PrimitiveConfig.Basic.foo);
		assertEquals("qux", PrimitiveConfig.Basic.bar);
	}

	@Test
	public void testArrays() {
		getProperty("arrays", "obj").set(new String[]{"5", "4", "3"});
		getProperty("arrays", "prim").set(new int[]{5, 4, 3});
		getProperty("arrays", "add").set(new int[]{5, 4, 3});
		getProperty("arrays", "ctor").set(new int[]{5, 4, 3});

		assertArrayEquals(new String[]{"5", "4", "3"}, PrimitiveConfig.Arrays.obj);
		assertArrayEquals(new int[]{5, 4, 3}, PrimitiveConfig.Arrays.prim);
		assertEquals(new HashSet<Integer>(Arrays.asList(5, 4, 3)), PrimitiveConfig.Arrays.add);
		assertEquals(new ThroughConstructor(new int[]{5, 4, 3}), PrimitiveConfig.Arrays.ctor);
	}

	private <T> Property<T> getProperty(String root, String... path) {
		Category current = null;
		for (Category child : PrimitiveConfigMetadata.categories()) {
			if (child.name.equals(root)) {
				current = child;
				break;
			}
		}

		for (int i = 0; i < path.length - 1; i++) {
			if (current == null) return null;
			for (Category child : current.children()) {
				if (child.name.equals(path[i])) {
					current = child;
					break;
				}
			}
		}

		if (current == null) return null;
		for (Property child : current.properties()) {
			if (child.name.equals(path[path.length - 1])) {
				return child;
			}
		}

		return null;
	}
}
