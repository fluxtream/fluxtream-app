package org.fluxtream.utils;

import java.util.Random;
import java.util.TimeZone;
import java.util.TreeSet;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * User: candide
 * Date: 21/05/13
 * Time: 11:48
 */
public class TimeSpanMapTest {

    @Test
    public void testLookup() {
        testTreeSetLookup();
        testTreeSetInsert();
    }

    @Test
    public void testAddTimespanSegment() {
        //// create an empty timespan map
        //TimespanMap segmentMap = new TimespanMap(DateTimeConstants.MILLIS_PER_DAY/2);
        //assertTrue(segmentMap.size()==1);
        //
        //// let's add a first datapoint
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-21T16:44:00+01:00", "2013-05-21T16:44:00+01:00", TimeZone.getMainTimeZone("Europe/Brussels")));
        //assertTrue(segmentMap.size() == 3);
        //
        //// let's add a second one in a different timezone
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T12:04:00Z", "2013-05-11T12:04:00Z", TimeZone.getMainTimeZone("Europe/London")));
        //assertTrue(segmentMap.size() == 5);
        //
        //// now add another datapoint that should be coalesced with the previous one
        //// note that the timezone is not the same city, but they are equivalent in terms of offset
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T20:04:00Z", "2013-05-11T20:04:00Z", TimeZone.getMainTimeZone("Europe/Dublin")));
        //final TimespanSegment<TimeZone> secondSegment = get(segmentMap, 1);
        //assertTrue(segmentMap.size() == 5);
        //assertTrue(secondSegment.duration()==8*3600000);
        //
        //// the next datapoint should not be coalesced
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-12T14:04:00Z", "2013-05-12T14:04:00Z", TimeZone.getMainTimeZone("Europe/Dublin")));
        //assertTrue(segmentMap.size() == 7);
        //assertTrue(secondSegment.duration() == 2 * 3600000);
        //
        //// let's now add a "real" timespan (not just a datapoint) that should coalesce everything so far in the Dublin/London timezone
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T13:33:00Z", "2013-05-12T06:04:00Z", TimeZone.getMainTimeZone("Europe/Dublin")));
        //assertTrue(segmentMap.size() == 5);
        //final TimespanSegment<TimeZone> firstSegment = get(segmentMap, 1);
        //assertTrue(firstSegment.duration() == 26 * 3600000);
    }

    @Test
    public void testAddTimespanSegment2() {
        //TimespanMap segmentMap = new TimespanMap(DateTimeConstants.MILLIS_PER_DAY/2);
        //assertTrue(segmentMap.size()==1);
        //
        ////add a few segments that should coalesce
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T10:00:00Z", "2013-05-11T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T12:00:00Z", "2013-05-11T13:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T14:00:00Z", "2013-05-11T15:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //
        //assertTrue(segmentMap.size()==3);
    }

    @Test
    public void testAddTimespanSegment3() {
        //TimespanMap segmentMap = new TimespanMap(DateTimeConstants.MILLIS_PER_DAY/2);
        //assertTrue(segmentMap.size()==1);
        //
        ////add a few segments that shouldn't coalesce
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T10:00:00Z", "2013-05-11T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-12T10:00:00Z", "2013-05-12T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-13T10:00:00Z", "2013-05-13T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //
        //assertTrue(segmentMap.size()==7);
    }

    @Test
    public void testAddTimespanSegment4() {
        //TimespanMap segmentMap = new TimespanMap(DateTimeConstants.MILLIS_PER_DAY/2);
        //assertTrue(segmentMap.size()==1);
        //
        ////add a few segments that shouldn't coalesce
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T10:00:00Z", "2013-05-11T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-12T10:00:00Z", "2013-05-12T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-13T10:00:00Z", "2013-05-13T11:00:00Z", TimeZone.getMainTimeZone("Europe/Brussels")));
        //
        //segmentMap.add(new TimespanSegment<TimeZone>("2013-05-11T10:30:00Z", "2013-05-13T11:30:00Z", TimeZone.getMainTimeZone("Europe/London")));
        //
        //assertTrue(segmentMap.size()==5);
        //final TimespanSegment<TimeZone> segment1 = get(segmentMap, 1);
        //assertTrue(segment1.value.getDisplayName().equals("Europe/Brussels"));
        //final TimespanSegment<TimeZone> segment2 = get(segmentMap, 2);
        //assertTrue(segment2.value.getDisplayName().equals("Europe/London"));
        //final TimespanSegment<TimeZone> segment3 = get(segmentMap, 3);
        //assertTrue(segment3.value.getDisplayName().equals("Europe/Brussels"));
    }

    //private TimespanSegment<TimeZone> get(final TimespanMap segmentMap, final int i) {
    //    final Iterator<TimespanSegment<TimeZone>> iterator = segmentMap.iterator();
    //    TimespanSegment<TimeZone> next = iterator.next();
    //    for(int j=0; j<i; j++)
    //        next = iterator.next();
    //    return next;
    //}

    private void testTreeSetLookup() {
        TreeSet<TimespanSegment<TimeZone>> spans = new TreeSet<TimespanSegment<TimeZone>>();
        for(int i=0; i<10000; i++)
            spans.add(new TimespanSegment<TimeZone>(i*10, 10));
        int times = 1000;
        double totalTimeTaken = 0;
        for (int i=0; i<times; i++) {
            long then = System.nanoTime();
            final TimespanSegment<TimeZone> lower = spans.lower(new TimespanSegment<TimeZone>(50000, 10));
            assertTrue(lower.start==49990);
            long now = System.nanoTime();
            totalTimeTaken += (now-then);
        }
        System.out.println(times + " lookups took " + totalTimeTaken + " ns (" + (totalTimeTaken / times) + " ns/lookup)");
    }


    private void testTreeSetInsert() {
        TreeSet<TimespanSegment<TimeZone>> spans = new TreeSet<TimespanSegment<TimeZone>>();
        for(int i=0; i<10000; i++)
            spans.add(new TimespanSegment<TimeZone>(i*10, 10));
        int times = 1000;
        double totalTimeTaken = 0;
        Random random = new Random();
        for (int i=0; i<times; i++) {
            long then = System.nanoTime();
            final int r = random.nextInt(50000);
            spans.add(new TimespanSegment<TimeZone>(r, 10));
            long now = System.nanoTime();
            totalTimeTaken += (now-then);
        }
        System.out.println(times + " inserts took " + totalTimeTaken + " ns (" + (totalTimeTaken / times) + " ns/insert)");
    }

}
