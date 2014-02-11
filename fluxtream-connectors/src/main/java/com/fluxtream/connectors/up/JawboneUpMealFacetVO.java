package com.fluxtream.connectors.up;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.TimeOfDayVO;
import com.fluxtream.domain.GuestSettings;
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

    @JsonRawValue
    public String servings;

    @Override
    protected void fromFacet(final JawboneUpMealFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        this.title = facet.title;
        this.date = facet.date;
        LocalDateTime localStartTime = new LocalDateTime(facet.start, DateTimeZone.forID(facet.tz));
        startTime = new TimeOfDayVO(localStartTime.getHourOfDay()*60+localStartTime.getMinuteOfHour(), true);

        LocalDateTime localEndTime = new LocalDateTime(facet.end, DateTimeZone.forID(facet.tz));
        endTime = new TimeOfDayVO(localEndTime.getHourOfDay()*60+localEndTime.getMinuteOfHour(), true);

        JSONArray servingsArray = new JSONArray();
        for (JawboneUpServingFacet serving : facet.servings) {
            JSONObject servingJSON = JSONObject.fromObject(serving.servingDetails);
            if (servingJSON.has("image")&&!servingJSON.getString("image").equals(""))
                servingJSON.put("image", JawboneUpVOHelper.getImageURL(servingJSON.getString("image"), facet, settings.config));
            servingsArray.add(servingJSON);
        }
        servings = servingsArray.toString();
    }

}
