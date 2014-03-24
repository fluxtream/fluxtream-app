package org.fluxtream.connectors.moves;

import org.junit.Test;

/**
 * User: candide
 * Date: 10/03/14
 * Time: 14:28
 */
public class MovesUpdaterTest {

    @Test
    public void testSublistOf(){
        MovesUpdater movesUpdater = new MovesUpdater();
        //List<String> dates = new ArrayList<String>();
        //String[] ds = new String[] {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15"};
        //for (String d : ds)
        //    dates.add(d);

        System.out.println(movesUpdater.localTimeStorageFormat.parseDateTime("20140312T172519+0100").getMillis());
    }

}
