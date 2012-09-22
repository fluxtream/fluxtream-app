package com.fluxtream;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import com.fluxtream.utils.TimeUtils;

public class TimeInterval {

	public long start, end;

	public TimeUnit timeUnit;
	public transient TimeZone timeZone;
	
	public TimeInterval(long start, long end, TimeUnit timeUnit, TimeZone timeZone) {
		this.start = start;
		this.end = end;
		this.timeUnit = timeUnit;
		this.timeZone = timeZone;
	}
	
	public boolean isMostRecent() {
		long now = System.currentTimeMillis();
		long fromMidnight = TimeUtils.fromMidnight(now, timeZone);
		return start>=fromMidnight;
	}
	
	public String toString() {
		return "[" + new Date(start) + ", " + new Date(end) + "]";
	}
	
	public boolean equals(Object o) {
		TimeInterval ti = (TimeInterval) o;
		return ti.start == start &&
				ti.end==end && ti.timeUnit==timeUnit &&
				ti.timeZone == timeZone;
	}

}
