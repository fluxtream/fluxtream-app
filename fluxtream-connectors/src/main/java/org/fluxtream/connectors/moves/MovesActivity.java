package org.fluxtream.connectors.moves;

import javax.persistence.Embeddable;

/**
 * User: candide
 * Date: 17/06/13
 * Time: 23:32
 */
@Embeddable

public class MovesActivity {

    String activityURI;
    public String activity;
    public String date;

    // Note that unlike everywhere else in the sysetm, startTimeStorage and endTimeStorage here are NOT local times.
    // They are in GMT.
    public String startTimeStorage, endTimeStorage;

    public long start, end;
    public int distance;
    public Integer steps;

    public String getActivityURI() {
        return activityURI;
    }
}
