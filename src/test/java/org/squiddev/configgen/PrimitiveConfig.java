package org.squiddev.configgen;

import java.util.HashSet;

@Config
public class PrimitiveConfig {
	/**
	 * All the things
	 */
	public final static class Section {
		/**
		 * Whatever
		 */
		@Range(min = 0, max = 10)
		@DefaultInt(3)
		public static int thing;

		@Range(min = 0, max = 10)
		@DefaultInt({3, 2, 3})
		public static int[] things;

		@Range(min = 0, max = 10)
		@DefaultInt({3, 2, 3})
		public static HashSet<Integer> hashSet;

		@Range(min = 0, max = 10)
		@DefaultInt({3, 2, 3})
		public static ThroughConstructor throughConstructor;

		/**
		 * Documentation
		 */
		@RequiresRestart
		public static class SubThing {
			@DefaultString("testing")
			public static String bar;

			@DefaultString({"1", "2", "3"})
			@RequiresRestart
			public static String[] bars;

			@Exclude
			public static double ignore;

			public static final double IGNORE = 0;
		}
	}

	public static void sync() {
		System.out.println("Syncing ...");
	}
}
