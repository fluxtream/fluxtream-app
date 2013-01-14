package com.fluxtream.connectors.runkeeper;

import java.util.Date;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RunKeeperFitnessActivityExtractorTest {

    @Test
    public void testDate() {
        DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");
        long millis = timeFormatter.parseMillis("Thu, 3 Mar 2011 07:00:00");
        System.out.println(new Date(millis));
    }

}
