package org.fluxtream.core;

import java.util.TimeZone;

/**
 * User: candide
 * Date: 12/06/13
 * Time: 14:08
 */
public interface TimeInterval {

    long getStart();

    long getEnd();

    TimeUnit getTimeUnit();

    @Deprecated
    TimeZone getMainTimeZone();

    TimeZone getTimeZone(long time) throws OutsideTimeBoundariesException;

    /**
     * get the timezone for a specific date
     * @param date in yyyy-mm-dd format
     * @return
     * @throws OutsideTimeBoundariesException
     */
    TimeZone getTimeZone(String date) throws OutsideTimeBoundariesException;

}
