package org.fluxtream.connectors.moves;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.plexus.util.StringUtils;
import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * User: candide
 * Date: 20/06/13
 * Time: 17:33
 */
@ApiModel(value="Moves Activity", description="")
public class MovesActivityVO {

    @ApiModelProperty(required=true)
    public String eventStart;
    @ApiModelProperty(required=true)
    public String eventEnd;
    @ApiModelProperty(required=true, value="Was this activity entered manually?")
    public boolean manual;
    @ApiModelProperty(required=true, value="This activity's human-readable name")
    public String activity;

    @ApiModelProperty(notes="For a complete list, please refer to https://dev.moves-app.com/docs/api_activity_list", value="The moves API code for this activity", required=true)
    public String activityCode = "generic";
    @ApiModelProperty(value="Generic Activity Code", allowableValues = "walking, cycling, transport, running", required=true)
    public String activityGroup;
    @ApiModelProperty(required=true)
    public String distance;
    @ApiModelProperty(required=true)
    public Integer steps;
    @ApiModelProperty(required=true)
    public DurationModel duration;
    @ApiModelProperty(required=true)
    public String date;
    @ApiModelProperty(required=true)
    public final String type = "moves-move-activity";

    private static final ArrayList<String> validActivityCodes = new ArrayList<String>(Arrays.asList(new String[]{"running", "walking", "cycling", "transport"}));

    public MovesActivityVO(MovesActivity activity, TimeZone timeZone,
                           long dateStart, long dateEnd,
                           GuestSettings settings, boolean doDateBoundsCheck) throws OutsideTimeBoundariesException {
        this.activity = StringUtils.capitalise(activity.activity);
        if (activity.activityGroup!=null&&validActivityCodes.contains(activity.activityGroup))
            this.activityCode = activity.activityGroup;
        else if (activity.activityGroup==null&&validActivityCodes.contains(activity.activity))
            this.activityCode = activity.activity;
        this.date = activity.date;
        this.eventStart = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forTimeZone(timeZone)).print(activity.start);
        this.eventEnd = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forTimeZone(timeZone)).print(activity.end);
        this.activityGroup = activity.activityGroup;
        this.manual = activity.manual!=null?activity.manual:false;

        // Potentially trucate to fit within the date
        long truncStartMilli = activity.start;
        long truncEndMilli = activity.end;
        boolean timeTruncated = false;

        // If we're doing date bounds checking, check if facet is entirely outside the time bounds of this date
        // If so, throw an exception so this facet isn't returned.  Otherwise potentially trim the start and end times
        if(doDateBoundsCheck) {
            if (activity.end<dateStart || activity.start>dateEnd) {
                throw new OutsideTimeBoundariesException();
            }

            // We know this facet overlaps the time bounds of date, check if it needs
            // to be truncated.
            if(activity.start<dateStart){
                truncStartMilli = dateStart;
                timeTruncated=true;
            }

            if(activity.end>=dateEnd) {
                truncEndMilli = dateEnd-1;
                timeTruncated=true;
            }
        }

        if (this.manual) {
            this.duration = new DurationModel(activity.duration);
        } else {
            // The args for creating a DurationModel are in seconds.
            // The units of start and end are milliseconds, so divide by 1000 to
            // calculate the duration in seconds to pass to the Duration Model.
            this.duration = new DurationModel((int)((truncEndMilli-truncStartMilli)/1000));

            // Note that the distance isn't going to be accurate here if we've done truncation
            // In that case, skip distance and steps for now
            if (activity.distance>0 && !timeTruncated) {
                if (settings.distanceMeasureUnit==GuestSettings.DistanceMeasureUnit.SI)
                    getMetricDistance(activity);
                else
                    getImperialdistance(activity);
            }
            if(activity.steps!=null && !timeTruncated) {
                this.steps = activity.steps;
            }
        }
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
