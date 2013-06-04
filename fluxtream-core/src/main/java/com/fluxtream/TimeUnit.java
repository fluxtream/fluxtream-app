package com.fluxtream;

public enum TimeUnit {

	DAY((byte)1), WEEK((byte)2), MONTH((byte)4), YEAR((byte)8);

    public static TimeUnit fromValue(String s) {
		for (TimeUnit timeUnit : values()) {
			if (timeUnit.toString().equalsIgnoreCase(s))
				return timeUnit;
		}
		return null;
	}

    private final byte bitPattern;

    public byte bitPattern() {
        return bitPattern;
    }

    TimeUnit(byte bitpattern) {
        this.bitPattern = bitpattern;
    }

}
