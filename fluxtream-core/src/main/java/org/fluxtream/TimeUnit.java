package org.fluxtream;

public enum TimeUnit {

	DAY, WEEK, MONTH, YEAR, ARBITRARY;

    public static TimeUnit fromValue(String s) {
		for (TimeUnit timeUnit : values()) {
			if (timeUnit.toString().equalsIgnoreCase(s))
				return timeUnit;
		}
		return null;
	}

}
