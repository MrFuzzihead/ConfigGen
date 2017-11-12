package org.squiddev.configgen;

import java.util.Arrays;

/**
 * Basic class for
 */
public class ThroughConstructor {
	private final int[] data;

	public ThroughConstructor(int[] data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object that) {
		return this == that || that instanceof ThroughConstructor && Arrays.equals(data, ((ThroughConstructor) that).data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}
}
