package org.fluxtream.core;

import java.util.Date;
import java.util.TimeZone;

public class SimpleTimeInterval implements TimeInterval {

    private transient TimeZone timeZone;
	
	public SimpleTimeInterval(long start, long end, TimeUnit timeUnit, TimeZone timeZone) {
        this.start = start;
        this.end = end;
        this.timeUnit = timeUnit;
        this.timeZone = timeZone;
	}
	
	public String toString() {
		return "[" + new Date(getStart()) + ", " + new Date(getEnd()) + "]";
	}
	
	public boolean equals(Object o) {
		TimeInterval ti = (TimeInterval) o;
		return ti.getStart() == getStart() &&
				ti.getEnd() == getEnd() && ti.getTimeUnit() == getTimeUnit() &&
				ti.getMainTimeZone() == getMainTimeZone();
	}

    @Override
    public TimeZone getMainTimeZone() {
        return timeZone;
    }

    @Override
    public TimeZone getTimeZone(final long time) throws OutsideTimeBoundariesException {
        return getMainTimeZone();
    }

    @Override
    public TimeZone getTimeZone(final String date) throws OutsideTimeBoundariesException {
        return getMainTimeZone();
    }

    private long start;
    private long end;
    private TimeUnit timeUnit;

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

}
