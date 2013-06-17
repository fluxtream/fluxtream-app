package com.fluxtream.connectors.moves;

import javax.persistence.Embeddable;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:32
 */
@Embeddable
public class Activity {

    public String activity;
    public String date;
    public int startMinute;
    public int endMinute;
    public int distance;
    public int steps;

}
