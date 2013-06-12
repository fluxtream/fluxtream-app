package com.fluxtream;

import java.util.TimeZone;

/**
 * User: candide
 * Date: 12/06/13
 * Time: 14:08
 */
public interface TimeInterval {
    boolean isMostRecent();

    long getStart();

    void setStart(long start);

    long getEnd();

    void setEnd(long end);

    TimeUnit getTimeUnit();

    void setTimeUnit(TimeUnit timeUnit);

    TimeZone getTimeZone();

    void setTimeZone(TimeZone timeZone);
}
