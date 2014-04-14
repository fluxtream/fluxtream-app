package org.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 * @author Prasanth Somasundar
 */
@Component("bodyMediaBurnJson")
public class BodyMediaBurnFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField ( final long guestId, AbstractFacet facet) {
        BodymediaBurnFacet burnFacet = (BodymediaBurnFacet) facet;
        if (burnFacet.json == null) {
            return Arrays.asList();
        }
        JSONArray burnJson = JSONArray.fromObject(burnFacet.json);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<burnJson.size(); i++) {
            JSONObject jsonRecord = burnJson.getJSONObject(i);
            long when = (facet.start/1000) + 60*i;
            final String source = jsonRecord.getString("source");
            final double mets = jsonRecord.getDouble("mets");
            final int caloriesBurned = jsonRecord.getInt("cals");
            final String activityType = jsonRecord.getString("activityType");
            List<Object> record = new ArrayList<Object>();
            record.add(when);
            record.add(onBody(source));
            record.add(mets);
            record.add(caloriesBurned);
            record.add(intensity(activityType));
            data.add(record);
        }
        final List<String> channelNames = Arrays.asList("onBody", "mets", "caloriesBurned", "activityType");

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(guestId, "BodyMedia", channelNames, data));
    }

    private int intensity(final String activityType) {
        if (activityType.equals("M"))
            return 1;
        else if (activityType.equals("V"))
            return 2;
        return 0;
    }

    private int onBody(final String source) {
        return source.equalsIgnoreCase("D")?1:0;
    }

}