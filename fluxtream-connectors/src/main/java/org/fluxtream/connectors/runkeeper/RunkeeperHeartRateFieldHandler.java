package org.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.services.impl.BodyTrackHelper;
import org.fluxtream.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
    public void handleField(final long guestId, final AbstractFacet facet) {
        RunKeeperFitnessActivityFacet activityFacet = (RunKeeperFitnessActivityFacet) facet;
        if (activityFacet.distanceStorage == null) {
            return;
        }
        JSONArray heartRateJson = JSONArray.fromObject(activityFacet.heartRateStorage);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<heartRateJson.size(); i++) {
            JSONObject record = heartRateJson.getJSONObject(i);
            final double heartRate = record.getInt("heart_rate");
            final double timestamp = record.getInt("timestamp");
            long when = (facet.start/1000) + (long)timestamp;
            List<Object> hrRecord = new ArrayList<Object>();
            hrRecord.add(when);
            hrRecord.add(heartRate);
            data.add(hrRecord);
        }
        final List<String> channelNames = Arrays.asList("heartRate");

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId, "runkeeper", channelNames, data);
    }

}
