package com.fluxtream.connectors.moves;

import java.util.Date;
import java.util.TimeZone;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:33
 */
public class MovesActivityVO {

    public final int startMinute;
    public final int endMinute;
    public String activity;
    public int distance;
    public Integer steps;

    public MovesActivityVO(MovesActivity activity, TimeZone timeZone){
        this.activity = activity.activity;
        this.startMinute = AbstractTimedFacetVO.toMinuteOfDay(new Date(activity.start), timeZone);
        this.endMinute = AbstractTimedFacetVO.toMinuteOfDay(new Date(activity.end), timeZone);
        this.distance = activity.distance;
        this.steps = activity.steps;
    }
}
