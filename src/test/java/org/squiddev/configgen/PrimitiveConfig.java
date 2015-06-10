package org.squiddev.configgen;

@Config
public class PrimitiveConfig {
	/**
	 * All the things
	 */
	public static class Section {
		/**
		 * Whatever
		 */
		@Range(min = 0, max = 10)
		public static int thing = 3;

		public static String bar;

		/**
		 * Documentation
		 */
		public static class SubThing {

		}

		static {
			bar = "HELLO";
		}

		public Section() {
			System.out.println("Things");
		}
	}

	public static void sync() {
	}
}
