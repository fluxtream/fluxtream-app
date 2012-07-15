package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("bodyMediaStepsJson")
public class BodyMediaStepsFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        BodymediaStepsFacet stepsFacet = (BodymediaStepsFacet) facet;
        if (stepsFacet.json == null) {
            return;
        }
        JSONArray stepsJson = JSONArray.fromObject(stepsFacet.json);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<stepsJson.size(); i++) {
            JSONObject stepsCount = stepsJson.getJSONObject(i);
            final int totalSteps = stepsCount.getInt("totalSteps");
            long when = (facet.start/1000) + (i*3600);
            List<Object> record = new ArrayList<Object>();
            record.add(when);
            record.add(totalSteps);
            data.add(record);
        }
        final List<String> channelNames = Arrays.asList("Steps_Graph");
        bodyTrackHelper.uploadToBodyTrack(guestId, "BodyMedia", channelNames, data);
    }

}
