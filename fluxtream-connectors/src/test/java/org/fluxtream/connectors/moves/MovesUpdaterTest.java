package org.fluxtream.connectors.moves;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

/**
 * User: candide
 * Date: 10/03/14
 * Time: 14:28
 */
public class MovesUpdaterTest {

    @Test
    public void testDateTimeFormat(){
        DateTimeFormatter timeStorageFormat = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
        String d = "20121212T160744+0200";
        System.out.println(timeStorageFormat.parseDateTime(d));
    }

}
