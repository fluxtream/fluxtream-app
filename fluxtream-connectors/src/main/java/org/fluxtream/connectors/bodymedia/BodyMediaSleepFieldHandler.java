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
@Component("bodyMediaSleepJson")
public class BodyMediaSleepFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField (final ApiKey apiKey, AbstractFacet facet) {
        BodymediaSleepFacet sleepFacet = (BodymediaSleepFacet) facet;
        if (sleepFacet.json == null) {
            return Arrays.asList();
        }
        JSONArray sleepJson = JSONArray.fromObject(sleepFacet.json);
        List<List<Object>> sleepingData = new ArrayList<List<Object>>();
        List<List<Object>> lyingData = new ArrayList<List<Object>>();
        int lastMinuteIndex = 0;
        for(int i=0; i<sleepJson.size(); i++) {
            JSONObject jsonRecord = sleepJson.getJSONObject(i);
            final int minuteIndex = jsonRecord.getInt("minuteIndex");
            boolean wasLying = (lastMinuteIndex==minuteIndex);
            if (!wasLying)
                addStandingRecord((facet.start/1000)+lastMinuteIndex*60, sleepingData, lyingData, (minuteIndex-lastMinuteIndex));

            final int duration = jsonRecord.getInt("duration");
            long when = (facet.start/1000) + minuteIndex*60;
            final String state = jsonRecord.getString("state");
            if (state.equals("LYING"))
                addLyingRecord(when, sleepingData, lyingData, duration);
            else if (state.equals("ASLEEP"))
                addSleepingRecord(when, sleepingData, lyingData, duration);

            lastMinuteIndex = minuteIndex+duration;
        }
        if (lastMinuteIndex<1440)
            addStandingRecord(facet.start/1000+lastMinuteIndex*60, sleepingData, lyingData, 1440-lastMinuteIndex);

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "BodyMedia", Arrays.asList("sleeping"), sleepingData),
                    bodyTrackHelper.uploadToBodyTrack(apiKey, "BodyMedia", Arrays.asList("lying"), lyingData));
    }

    private void addSleepingRecord(long when, List<List<Object>> sleepingData, List<List<Object>> lyingData, int duration) {
        for (int i=0; i<duration; i++) {
            List<Object> sleepingRecord = new ArrayList<Object>();
            sleepingRecord.add(when+i*60);
            sleepingRecord.add(1);
            sleepingData.add(sleepingRecord);
            lyingData.add(sleepingRecord);
        }
    }

    private void addLyingRecord(long when, List<List<Object>> sleepingData, List<List<Object>> lyingData, int duration) {
        for (int i=0; i<duration; i++) {
            List<Object> lyingRecord = new ArrayList<Object>();
            lyingRecord.add(when+i*60);
            lyingRecord.add(1);
            lyingData.add(lyingRecord);
            List<Object> sleepingRecord = new ArrayList<Object>();
            sleepingRecord.add(when+i*60);
            sleepingRecord.add(0);
            sleepingData.add(sleepingRecord);
        }
    }

    private void addStandingRecord(long when, List<List<Object>> sleepingData, List<List<Object>> lyingData, int duration) {
        for (int i=0; i<duration; i++) {
            List<Object> standingRecord = new ArrayList<Object>();
            standingRecord.add(when+i*60);
            standingRecord.add(0);
            lyingData.add(standingRecord);
            sleepingData.add(standingRecord);
        }
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 4, apiKey.getConnector().getDeviceNickname(), "sleeping", channelMappings);
        ChannelMapping.addToDeclaredMappings(apiKey, 4, apiKey.getConnector().getDeviceNickname(), "lying", channelMappings);
    }

}
