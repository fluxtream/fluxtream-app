package org.fluxtream;

import java.util.Map;
import java.util.TimeZone;
import org.fluxtream.utils.TimespanSegment;
import org.joda.time.DateTimeZone;

/**
 * User: candide
 * Date: 02/10/13
 * Time: 18:21
 */
public class TimezoneAwareTimeInterval implements TimeInterval {

    long start, end;
    TimeUnit timeUnit;
    TimezoneMap timezoneMap;
    Map<String, TimeZone> consensusTimezones;

    public TimezoneAwareTimeInterval(long start, long end,
                                     TimeUnit timeUnit,
                                     Map<String, TimeZone> consensusTimezones,
                                     TimezoneMap timezoneMap) {
        this.start = start;
        this.end = end;
        this.timeUnit = timeUnit;
        this.timezoneMap = timezoneMap;
        this.consensusTimezones = consensusTimezones;
    }

    @Override
    public TimeZone getMainTimeZone() {
        return timezoneMap.getMainTimezone().toTimeZone();
    }

    @Override
    public long getStart() { return start; };

    @Override
    public long getEnd() { return end; }

    @Override
    public TimeUnit getTimeUnit() { return timeUnit; }

    @Override
    public TimeZone getTimeZone(final long time) throws OutsideTimeBoundariesException {
        final TimespanSegment<DateTimeZone> dateTimeZoneTimespanSegment = timezoneMap.queryPoint(time);
        return dateTimeZoneTimespanSegment.getValue().toTimeZone();
    }

    @Override
    public TimeZone getTimeZone(final String date) throws OutsideTimeBoundariesException {
        return consensusTimezones.get(date);
    }
}
