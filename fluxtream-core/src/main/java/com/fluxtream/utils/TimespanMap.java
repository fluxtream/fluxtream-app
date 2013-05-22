package com.fluxtream.utils;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * User: candide
 * Date: 21/05/13
 * Time: 11:33
 */
public class TimespanMap<T> {

    private long end, start;
    private int coalesceMillis;
    TreeSet<TimespanSegment<T>> spans = new TreeSet<TimespanSegment<T>>();

    public TimespanMap(long start, long end, final int i) {
        coalesceMillis = i;
        this.start = start;
        this.end = end;
        spans.add(new TimespanSegment<T>(this.start, this.end));
    }

    public TimespanMap(final int i) {
        this(Long.MIN_VALUE, Long.MAX_VALUE, i);
    }

    public void add(TimespanSegment<T> segment) {
        final TimespanSegment<T> startLower = spans.floor(segment);
        TimespanSegment endSegment = new TimespanSegment(segment.end, segment.end);
        final TimespanSegment<T> endLower = spans.floor(endSegment);
        if (startLower==endLower) {
        } else {

        }
    }

    public int size() {
        return spans.size();
    }

    public Iterator<TimespanSegment<T>> iterator() {
        final Iterator<TimespanSegment<T>> iterator = spans.iterator();
        return iterator;
    }
}
