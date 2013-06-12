package com.fluxtream;

import java.util.Date;
import java.util.TimeZone;
import com.fluxtream.utils.TimeUtils;

public class SimpleTimeInterval implements TimeInterval {

	private long start;
    private long end;

	private TimeUnit timeUnit;
	private transient TimeZone timeZone;
	
	public SimpleTimeInterval(long start, long end, TimeUnit timeUnit, TimeZone timeZone) {
		this.setStart(start);
		this.setEnd(end);
		this.setTimeUnit(timeUnit);
		this.setTimeZone(timeZone);
	}
	
	@Override
    public boolean isMostRecent() {
		long now = System.currentTimeMillis();
		long fromMidnight = TimeUtils.fromMidnight(now, getMainTimeZone());
		return getStart() >=fromMidnight;
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
    public long getStart() {
        return start;
    }

    @Override
    public void setStart(final long start) {
        this.start = start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public void setEnd(final long end) {
        this.end = end;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public void setTimeUnit(final TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public TimeZone getMainTimeZone() {
        return timeZone;
    }

    @Override
    public void setTimeZone(final TimeZone timeZone) {
        this.timeZone = timeZone;
    }
}
