package org.fluxtream.core.utils;

import java.util.TimeZone;

/**
 * User: candide
 * Date: 21/05/13
 * Time: 11:25
 */
public class TimespanSegment<T> implements Comparable<TimespanSegment>{

    private static final int SIMULTANEOUS = 0;
    private static final int AFTER = 1;
    private static final int BEFORE = -1;

    long start;
    long end;
    T value;

    public TimespanSegment(final String start, final String end) {
        this.start = javax.xml.bind.DatatypeConverter.parseDateTime(start).getTimeInMillis();
        this.end = javax.xml.bind.DatatypeConverter.parseDateTime(end).getTimeInMillis();
    }

    public TimespanSegment(final String start, final String end, T value) {
        this.start = javax.xml.bind.DatatypeConverter.parseDateTime(start).getTimeInMillis();
        this.end = javax.xml.bind.DatatypeConverter.parseDateTime(end).getTimeInMillis();
        this.value = value;
    }

    public TimespanSegment(final long start, final long end) {
        this.start = start;
        this.end = end;
    }

    public TimespanSegment(final long start, final long end, T value) {
        this.start = start;
        this.end = end;
        this.value = value;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public T getValue() {
        return value;
    }

    public long duration() {
        return end-start;
    }

    public boolean isTimeInSpan(long ts) {
        return (ts>=start && ts<end);
    }


    @Override
    public int compareTo(final TimespanSegment o) {
        final long startTimeDifference = this.start - o.start;
        if(startTimeDifference>0)
            return AFTER;
        else if (startTimeDifference<0)
            return BEFORE;
        else return SIMULTANEOUS;
    }

    public int compareEnd(final TimespanSegment o) {
        final long timeDifference = this.end - o.start;
        if(timeDifference>0)
            return AFTER;
        else if (timeDifference<0)
            return BEFORE;
        else return SIMULTANEOUS;
    }

    public static void main(final String[] args) {
        TimeZone london = TimeZone.getTimeZone("Europe/London");
        TimeZone dublin = TimeZone.getTimeZone("Europe/Dublin");
        System.out.println(london.getRawOffset()==dublin.getRawOffset());
    }

    public void setStart(final long start) {
        this.start = start;
    }
}
