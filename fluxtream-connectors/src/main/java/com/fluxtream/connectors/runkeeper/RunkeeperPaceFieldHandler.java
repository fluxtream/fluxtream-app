package com.fluxtream.connectors.runkeeper;

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
 * User: candide
 * Date: 21/10/13
 * Time: 00:28
 */
@Component("runkeeperPace")
public class RunkeeperPaceFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        RunKeeperFitnessActivityFacet activityFacet = (RunKeeperFitnessActivityFacet) facet;
        if (activityFacet.distanceStorage == null) {
            return;
        }
        JSONArray distanceJson = JSONArray.fromObject(activityFacet.distanceStorage);
        List<List<Object>> data = new ArrayList<List<Object>>();
        double lastTimestamp = 0d;
        double lastDistance = 0d;
        for(int i=0; i<distanceJson.size(); i++) {
            JSONObject record = distanceJson.getJSONObject(i);
            final double totalDistance = record.getInt("distance");
            final double timestamp = record.getInt("timestamp");
            final double lap = timestamp - lastTimestamp;
            final double distance = totalDistance - lastDistance;
            lastTimestamp = timestamp;
            lastDistance = totalDistance;
            if (distance==0||lap==0)
                continue;
            final double minutesPerKilometer = ((1000d/distance)*lap)/60d;
            long when = (facet.start/1000) + (long)timestamp;
            for (int j=0; j<(int)lap; j++) {
                when += j;
                List<Object> siRecord = new ArrayList<Object>();
                siRecord.add(when);
                siRecord.add(minutesPerKilometer);
                siRecord.add(minutesPerKilometer/.621371192d);
                data.add(siRecord);
            }
        }
        final List<String> channelNames = Arrays.asList("minutesPerKilometer", "minutesPerMile");

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId, "Runkeeper", channelNames, data);
    }

}
