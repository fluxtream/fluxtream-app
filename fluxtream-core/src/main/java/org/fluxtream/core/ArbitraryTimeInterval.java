package org.fluxtream.core;

import java.util.TimeZone;

/**
 * User: candide
 * Date: 02/10/13
 * Time: 16:11
 */
public class ArbitraryTimeInterval implements TimeInterval {



    @Override
    public long getStart() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getEnd() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TimeUnit getTimeUnit() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TimeZone getMainTimeZone() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TimeZone getTimeZone(final long time) throws OutsideTimeBoundariesException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TimeZone getTimeZone(final String date) throws OutsideTimeBoundariesException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
