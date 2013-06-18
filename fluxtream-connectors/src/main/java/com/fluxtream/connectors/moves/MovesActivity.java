package com.fluxtream.connectors.moves;

import javax.persistence.Embeddable;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:32
 */
@Embeddable

public class MovesActivity {

    String activityId;
    public String activity;
    public String date;
    public String startTimeStorage, endTimeStorage;
    public long start, end;
    public int distance;
    public Integer steps;

    public String getActivityId() {
        return activityId;
    }
}
