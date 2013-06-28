package com.fluxtream.connectors.moves;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.text.DecimalFormat;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.connectors.vos.TimeOfDayVO;
import com.fluxtream.domain.GuestSettings;
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
    public String activity, activityCode;
    public String distance;
    public Integer steps;
    public DurationModel duration;
    public TimeOfDayVO startTime, endTime;
    public String date;
    public final String type = "moves-move-activity";

    public MovesActivityVO(MovesActivity activity, TimeZone timeZone, GuestSettings settings){
        this.activity = activityDict.get(activity.activity);
        this.activityCode = activity.activity;
        this.startMinute = AbstractTimedFacetVO.toMinuteOfDay(new Date(activity.start), timeZone);
        this.endMinute = AbstractTimedFacetVO.toMinuteOfDay(new Date(activity.end), timeZone);
        if (activity.distance>0) {
            if (settings.distanceMeasureUnit==GuestSettings.DistanceMeasureUnit.SI)
                getMetricDistance(activity);
            else
                getImperialdistance(activity);
        }
        this.date = activity.date;
        this.steps = activity.steps;
        this.startTime = new TimeOfDayVO(this.startMinute, true);
        this.endTime = new TimeOfDayVO(this.endMinute, true);
        // The args for creating a DurationModel are in seconds.
        // The units of start and end are milliseconds, so divide by 1000 to
        // calculate the duration in seconds to pass to the Duration Model.
        this.duration = new DurationModel((int)((activity.end-activity.start)/1000));
    }

    private void getImperialdistance(final MovesActivity activity) {
        double yards = activity.distance / 0.9144;
        double miles = activity.distance * 0.00062137119;

        if (miles>1) {
            DecimalFormat df = new DecimalFormat("0.#");
            String mstr = df.format(miles);
            if(mstr.contentEquals("1")) {
                this.distance = "1 mile";
            }
            else {
                this.distance = mstr + " miles";
            }
        }
        else {
            int yint = (int)(round(yards));
            if(yint == 1) {
                this.distance = "1 yard";
            }
            else {
                this.distance = yint + " yards";
            }
        }
    }

    private void getMetricDistance(final MovesActivity activity) {
        if (activity.distance>1000) {
            double km = round((double)activity.distance/1000d);
            this.distance = km + " km";
        } else {
            this.distance = activity.distance + " m";
        }
    }

    double round(double v) {
        return (double) Math.round(v * 100) / 100;
    }
}
