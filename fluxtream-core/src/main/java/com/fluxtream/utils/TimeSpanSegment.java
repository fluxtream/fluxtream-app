package com.fluxtream.utils;

import java.util.TimeZone;

/**
 * User: candide
 * Date: 21/05/13
 * Time: 11:25
 */
public class TimespanSegment<T> implements Comparable<TimespanSegment>{

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

    public long duration() {
        return end-start;
    }

    @Override
    public int compareTo(final TimespanSegment o) {
        final long startTimeDifference = this.start - o.start;
        if(startTimeDifference>0)
            return 1;
        else if (startTimeDifference<0)
            return -1;
        else return 0;
    }

    public static void main(final String[] args) {
        TimeZone london = TimeZone.getTimeZone("Europe/London");
        TimeZone dublin = TimeZone.getTimeZone("Europe/Dublin");
        System.out.println(london.getRawOffset()==dublin.getRawOffset());
    }

}
