package org.fluxtream.core.utils;

import java.util.Random;

public class RandomString {

	private static final char[] symbols = new char[36];

	static {
		for (int idx = 0; idx < 10; ++idx)
			symbols[idx] = (char) ('0' + idx);
		for (int idx = 10; idx < 36; ++idx)
			symbols[idx] = (char) ('a' + idx - 10);
	}

	private final Random random = new Random();

	private final char[] buf;

	public RandomString(int length) {
		if (length < 1)
			throw new IllegalArgumentException("length < 1: " + length);
		buf = new char[length];
	}
	
	private int nextRandom() {
		synchronized(random) {
			return random.nextInt(symbols.length);
		}
	}

	public String nextString() {
		synchronized (buf) {
			for (int idx = 0; idx < buf.length; ++idx)
				buf[idx] = symbols[nextRandom()];
		}
		return new String(buf);
	}

}
