package com.fluxtream.connectors.moves;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.connectors.vos.TimeOfDayVO;
import com.fluxtream.mvc.models.DurationModel;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:33
 */
public class MovesActivityVO {

    private static Map<String,String> activityDict = new HashMap<String,String>();
    static {
        activityDict.put("wlk", "Walking");
        activityDict.put("cyc", "Cycling");
        activityDict.put("run", "Running");
        activityDict.put("trp", "Transport");
    }

    public final int startMinute;
    public final int endMinute;
    public String activity;
    public int distance;
    public Integer steps;
    public DurationModel duration;
    public TimeOfDayVO startTime, endTime;

    public MovesActivityVO(MovesActivity activity, TimeZone timeZone){
        this.activity = activityDict.get(activity.activity);
        this.startMinute = AbstractTimedFacetVO.toMinuteOfDay(new Date(activity.start), timeZone);
        this.endMinute = AbstractTimedFacetVO.toMinuteOfDay(new Date(activity.end), timeZone);
        this.distance = activity.distance;
        this.steps = activity.steps;
        this.startTime = new TimeOfDayVO(this.startMinute, true);
        this.endTime = new TimeOfDayVO(this.endMinute, true);
        this.duration = new DurationModel((int)(activity.end-activity.start));
    }
}
