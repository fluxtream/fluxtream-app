package org.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * User: candide
 * Date: 21/10/13
 * Time: 00:26
 */
@Component("runkeeperHeartRate")
public class RunkeeperHeartRateFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField(final ApiKey apiKey, final AbstractFacet facet) {
        RunKeeperFitnessActivityFacet activityFacet = (RunKeeperFitnessActivityFacet) facet;
        if (activityFacet.distanceStorage == null) {
            return Arrays.asList();
        }
        JSONArray heartRateJson = JSONArray.fromObject(activityFacet.heartRateStorage);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<heartRateJson.size(); i++) {
            JSONObject record = heartRateJson.getJSONObject(i);
            final double heartRate = record.getInt("heart_rate");
            final double timestamp =
                    record.getInt("timestamp");
            long when = (facet.start/1000) + (long)timestamp;
            List<Object> hrRecord = new ArrayList<Object>();
            hrRecord.add(when);
            hrRecord.add(heartRate);
            data.add(hrRecord);
        }
        final List<String> channelNames = Arrays.asList("heartRate");

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "runkeeper", channelNames, data));
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping channelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.data,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "fitnessActivity").value(),
                apiKey.getConnector().getDeviceNickname(), "heartRate",
                apiKey.getConnector().getDeviceNickname(), "heartRate");
        channelMappings.add(channelMapping);
    }

}
