package org.squiddev.configgen;

@Config(languagePrefix = "testing:")
public class PrimitiveConfig {
	public final static class Basic {
		public static int foo;

		public static String bar;
	}

	public final static class Defaults {
		@DefaultInt(3)
		public static int foo;

		@DefaultString("bar")
		public static String bar;
	}

	/**
	 * Checks documentation works
	 */
	public final static class Documentation {
		/**
		 * This should create some very nice documentation
		 * you know.
		 */
		public static int foo;
	}

	@RequiresRestart
	public static class Annotations {
		public static String foo;

		@RequiresRestart
		public static String bar;

		@Range(min = 0, max = 10)
		public static int range;

		@Range(min = 0, max = 10)
		public static double doubleRange;

		@Exclude
		public static double ignore;

		public static final double IGNORE = 0;

		@Exclude
		public static class IgnoreMe {
			public static String broken;
		}
	}

	public final static class Arrays {
		@DefaultString({"1", "2", "3"})
		public static String[] foo;

		public static String[] bar;

		@DefaultInt({1, 2, 3})
		public static int[] primitiveFoo;

		public static int[] primitiveBar;
	}

	@OnSync
	public static void saveTheStuff() {
		System.out.println("Syncing ...");
	}
}
