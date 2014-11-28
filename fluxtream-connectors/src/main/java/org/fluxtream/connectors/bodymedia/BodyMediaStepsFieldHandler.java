package org.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
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
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField (final ApiKey apiKey, AbstractFacet facet) {
        BodymediaStepsFacet stepsFacet = (BodymediaStepsFacet) facet;
        if (stepsFacet.json == null) {
            return Arrays.asList();
        }
        JSONArray stepsJson = JSONArray.fromObject(stepsFacet.json);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<stepsJson.size(); i++) {
            JSONObject stepsCount = stepsJson.getJSONObject(i);
            final int totalSteps = stepsCount.getInt("totalSteps");
            // Put the steps in the middle of the hour they're for rather
            // than at the beginning.  That way the steps are  centered
            // with respect to the hour with the matching mets peaks
            // rather than preceeding them
            long when = (facet.start/1000) + (i*3600) + 1800;
            List<Object> record = new ArrayList<Object>();
            record.add(when);
            record.add(totalSteps);
            data.add(record);
        }
        final List<String> channelNames = Arrays.asList("stepsGraph");

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "BodyMedia", channelNames, data));
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 2, apiKey.getConnector().getDeviceNickname(), "stepsGraph", channelMappings);
    }

}
