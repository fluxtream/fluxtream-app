package com.fluxtream.utils;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * User: candide
 * Date: 21/05/13
 * Time: 11:33
 */
public class TimespanMap<T> {

    private int coalesceMillis;
    TreeSet<TimespanSegment<T>> spans = new TreeSet<TimespanSegment<T>>();

    public TimespanMap(long start, long end, final int i) {
        coalesceMillis = i;
        spans.add(new TimespanSegment<T>(start, end));
    }

    public TimespanMap(final int i) {
        coalesceMillis = i;
        spans.add(new TimespanSegment<T>(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    public void add(TimespanSegment<T> segment) {

    }

    public int size() {
        return spans.size();
    }

    public Iterator<TimespanSegment<T>> iterator() {
        final Iterator<TimespanSegment<T>> iterator = spans.iterator();
        return iterator;
    }
}
