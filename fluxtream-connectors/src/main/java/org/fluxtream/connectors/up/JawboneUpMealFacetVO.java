package org.fluxtream.connectors.up;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractFacetVO;
import org.fluxtream.connectors.vos.TimeOfDayVO;
import org.fluxtream.domain.GuestSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

/**
 * User: candide
 * Date: 11/02/14
 * Time: 15:05
 */
public class JawboneUpMealFacetVO extends AbstractFacetVO<JawboneUpMealFacet> {

    public String title;
    public TimeOfDayVO startTime;
    public TimeOfDayVO endTime;
    public int startMinute, endMinute;
    public long start, end;

    @JsonRawValue
    public String servings;

    @Override
    protected void fromFacet(final JawboneUpMealFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.title = facet.title;
        this.date = facet.date;
        this.start = facet.start;
        this.end = facet.end;

        LocalDateTime localStartTime = new LocalDateTime(facet.start, DateTimeZone.forID(facet.tz));
        startMinute = localStartTime.getHourOfDay() * 60 + localStartTime.getMinuteOfHour();
        startTime = new TimeOfDayVO(startMinute, true);

        LocalDateTime localEndTime = new LocalDateTime(facet.end, DateTimeZone.forID(facet.tz));
        endMinute = localEndTime.getHourOfDay() * 60 + localEndTime.getMinuteOfHour();
        endTime = new TimeOfDayVO(endMinute, true);

        JSONArray servingsArray = new JSONArray();
        for (JawboneUpServingFacet serving : facet.getServings()) {
            JSONObject servingJSON = JSONObject.fromObject(serving.servingDetails);
            servingJSON.accumulate("deviceName", "Jawbone_UP");
            servingJSON.accumulate("channelName", "serving");
            servingJSON.accumulate("UID", serving.getId());
            servingJSON.accumulate("start", serving.start);
            if (servingJSON.has("image")&&!servingJSON.getString("image").equals(""))
                servingJSON.put("image", JawboneUpVOHelper.getImageURL(servingJSON.getString("image"), facet, settings.config));
            servingsArray.add(servingJSON);
        }
        servings = servingsArray.toString();
    }

}
