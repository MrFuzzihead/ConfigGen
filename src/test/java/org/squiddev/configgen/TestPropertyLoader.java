package org.squiddev.configgen;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestPropertyLoader {
	@Test
	public void testBasic() throws Exception {
		System.setProperty("PrimitiveConfig.Basic.foo", "123");
		System.setProperty("PrimitiveConfig.Basic.bar", "hello");
		load(PrimitiveConfig.class);

		assertEquals(123, PrimitiveConfig.Basic.foo);
		assertEquals("hello", PrimitiveConfig.Basic.bar);
	}

	@Test
	public void testDefaultWithout() throws Exception {
		System.clearProperty("PrimitiveConfig.Defaults.foo");
		System.clearProperty("PrimitiveConfig.Defaults.bar");
		load(PrimitiveConfig.class);

		assertEquals(3, PrimitiveConfig.Defaults.foo);
		assertEquals("bar", PrimitiveConfig.Defaults.bar);
	}

	@Test
	public void testDefaultWith() throws Exception {
		System.setProperty("PrimitiveConfig.Defaults.foo", "4");
		System.setProperty("PrimitiveConfig.Defaults.bar", "qux");
		load(PrimitiveConfig.class);

		assertEquals(4, PrimitiveConfig.Defaults.foo);
		assertEquals("qux", PrimitiveConfig.Defaults.bar);
	}

	@Test
	public void testArrayDefaultWithout() throws Exception {
		System.clearProperty("PrimitiveConfig.Arrays.objDef");
		System.clearProperty("PrimitiveConfig.Arrays.primDef");
		System.clearProperty("PrimitiveConfig.Arrays.addDef");
		System.clearProperty("PrimitiveConfig.Arrays.ctorDef");

		load(PrimitiveConfig.class);

		assertArrayEquals(new String[]{"1", "2", "3"}, PrimitiveConfig.Arrays.objDef);
		assertArrayEquals(new int[]{1, 2, 3}, PrimitiveConfig.Arrays.primDef);
		assertEquals(new HashSet<Integer>(Arrays.asList(1, 2, 3)), PrimitiveConfig.Arrays.addDef);
		assertEquals(new ThroughConstructor(new int[]{1, 2, 3}), PrimitiveConfig.Arrays.ctorDef);
	}

	@Test
	public void testArrayDefaultWith() throws Exception {
		System.setProperty("PrimitiveConfig.Arrays.objDef", "5,4,3");
		System.setProperty("PrimitiveConfig.Arrays.primDef", "5,4,3");
		System.setProperty("PrimitiveConfig.Arrays.addDef", "5,4,3");
		System.setProperty("PrimitiveConfig.Arrays.ctorDef", "5,4,3");

		load(PrimitiveConfig.class);

		assertArrayEquals(new String[]{"5", "4", "3"}, PrimitiveConfig.Arrays.objDef);
		assertArrayEquals(new int[]{5, 4, 3}, PrimitiveConfig.Arrays.primDef);
		assertEquals(new HashSet<Integer>(Arrays.asList(5, 4, 3)), PrimitiveConfig.Arrays.addDef);
		assertEquals(new ThroughConstructor(new int[]{5, 4, 3}), PrimitiveConfig.Arrays.ctorDef);
	}

	@Test
	public void testArrayWith() throws Exception {
		System.setProperty("PrimitiveConfig.Arrays.obj", "5,4,3");
		System.setProperty("PrimitiveConfig.Arrays.prim", "5,4,3");
		System.setProperty("PrimitiveConfig.Arrays.add", "5,4,3");
		System.setProperty("PrimitiveConfig.Arrays.ctor", "5,4,3");

		load(PrimitiveConfig.class);

		assertArrayEquals(new String[]{"5", "4", "3"}, PrimitiveConfig.Arrays.obj);
		assertArrayEquals(new int[]{5, 4, 3}, PrimitiveConfig.Arrays.prim);
		assertEquals(new HashSet<Integer>(Arrays.asList(5, 4, 3)), PrimitiveConfig.Arrays.add);
		assertEquals(new ThroughConstructor(new int[]{5, 4, 3}), PrimitiveConfig.Arrays.ctor);
	}

	private static void load(Class<?> klass) {
		try {
			klass
				.getClassLoader()
				.loadClass(klass.getName() + "PropertyLoader")
				.getMethod("init")
				.invoke(null);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
