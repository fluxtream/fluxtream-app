package org.fluxtream.connectors.bodymedia;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.utils.TimeUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.hibernate.search.annotations.Indexed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Stores data from the bodymedia burn api
 */
@Entity(name="Facet_BodymediaBurn")
@ObjectTypeSpec(name = "burn", value = 1, prettyname = "Calories Burned", isDateBased = true)
@NamedQueries({
    @NamedQuery(name = "bodymedia.burn.getFailedUpdate", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.lastSync=1"),
    @NamedQuery(name = "bodymedia.burn.getDaysPrior", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? AND facet.start<? ORDER BY facet.start DESC"),
    @NamedQuery(name = "bodymedia.burn.getByLastSync", query = "SELECT facet FROM Facet_BodymediaBurn facet WHERE facet.guestId=? ORDER BY facet.lastSync DESC")
})
@Indexed
public class BodymediaBurnFacet extends BodymediaAbstractFacet {

    public int totalCalories = 0;
    public int estimatedCalories = 0;
    public int predictedCalories = 0;

    public BodymediaBurnFacet() {
        super();
    }

    public BodymediaBurnFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    public void setTotalCalories(final int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public void setEstimatedCalories(final int estimatedCalories) {
        this.estimatedCalories = estimatedCalories;
    }

    public void setPredictedCalories(final int predictedCalories) {
        this.predictedCalories = predictedCalories;
    }

    public static AbstractFacet createOrUpdateDay(BodymediaAbstractFacet existing, final UpdateInfo updateInfo, JSONObject burnJson, DateTimeZone timeZone) {
        /* burnJson is a JSONArray that contains a seperate JSONArray and calorie counts for each day
        */
        DateTimeFormatter syncTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
        BodymediaBurnFacet facet=null;

        if (existing == null) {
            facet = new BodymediaBurnFacet(updateInfo.apiKey.getId());
            facet.guestId = updateInfo.apiKey.getGuestId();
            facet.api = updateInfo.apiKey.getConnector().value();
        }
        else {
            facet = (BodymediaBurnFacet)existing;
        }

        facet.timeUpdated = System.currentTimeMillis();

        if(burnJson.has("days") && burnJson.has("lastSync"))
        {
            DateTime d = syncTimeFormatter.parseDateTime(burnJson.getJSONObject("lastSync").getString("dateTime"));
            JSONArray daysArray = burnJson.getJSONArray("days");
            if(daysArray.size()!=1)
                throw new RuntimeException("days array is not the right length: expected 1, got "+ String.valueOf(daysArray.size()));

            JSONObject day = daysArray.getJSONObject(0);
            facet.setTotalCalories(day.getInt("totalCalories"));
            facet.date = day.getString("date");
            facet.setEstimatedCalories(day.getInt("estimatedCalories"));
            facet.setPredictedCalories(day.getInt("predictedCalories"));
            facet.json = day.getString("minutes");
            facet.lastSync = d.getMillis();

            DateTime date = formatter.parseDateTime(day.getString("date"));
            facet.date = TimeUtils.dateFormatter.print(date.getMillis());

            long fromMidnight = TimeUtils.fromMidnight(date.getMillis(), timeZone.toTimeZone());
            long toMidnight = TimeUtils.toMidnight(date.getMillis(), timeZone.toTimeZone());
            //Sets the start and end times for the facet so that it can be uniquely defined
            facet.start = fromMidnight;
            facet.end = toMidnight;
        }
        else
            throw new RuntimeException("days array is not a proper JSONObject");

        return facet;
    }
}