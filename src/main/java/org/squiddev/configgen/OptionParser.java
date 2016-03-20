package org.squiddev.configgen;

/**
 * Parses {@link System#getProperty(String)} properties
 */
public final class OptionParser {
	private OptionParser() {
	}

	public static String getString(String name, String def) {
		return System.getProperty(name, def);
	}

	public static int getInt(String name, int def) {
		String value = System.getProperty(name);
		return value == null ? def : Integer.parseInt(value);
	}

	public static double getDouble(String name, double def) {
		String value = System.getProperty(name);
		return value == null ? def : Double.parseDouble(value);
	}

	public static boolean getBoolean(String name, boolean def) {
		String value = System.getProperty(name);
		return value == null ? def : Boolean.parseBoolean(value);
	}

	public static String[] getStringList(String name, String[] def) {
		String value = System.getProperty(name);
		if (value == null) {
			return def;
		} else if (value.isEmpty()) {
			return new String[0];
		} else {
			return value.split(",");
		}
	}

	public static int[] getIntList(String name, int[] def) {
		String value = System.getProperty(name);
		if (value == null) {
			return def;
		} else if (value.isEmpty()) {
			return new int[0];
		} else {
			String[] values = value.split(",");
			int[] outs = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				outs[i] = Integer.parseInt(values[i]);
			}
			return outs;
		}
	}

	public static double[] getDoubleList(String name, double[] def) {
		String value = System.getProperty(name);
		if (value == null) {
			return def;
		} else if (value.isEmpty()) {
			return new double[0];
		} else {
			String[] values = value.split(",");
			double[] outs = new double[values.length];
			for (int i = 0; i < values.length; i++) {
				outs[i] = Double.parseDouble(values[i]);
			}
			return outs;
		}
	}

	public static boolean[] getBooleanList(String name, boolean[] def) {
		String value = System.getProperty(name);
		if (value == null) {
			return def;
		} else if (value.isEmpty()) {
			return new boolean[0];
		} else {
			String[] values = value.split(",");
			boolean[] outs = new boolean[values.length];
			for (int i = 0; i < values.length; i++) {
				outs[i] = Boolean.parseBoolean(values[i]);
			}
			return outs;
		}
	}
}
