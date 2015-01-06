package org.fluxtream.connectors.beddit;

import com.google.gdata.util.common.base.Pair;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("bedditHeartRate")
public class BedditHeartRateFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField(ApiKey apiKey, AbstractFacet facet) {
        SleepFacet sleepFacet = (SleepFacet) facet;
        List<List<Object>> data = new ArrayList<List<Object>>();
        if (sleepFacet.heartRateCurveData!=null) {
            List<Pair<Long,Double>> sleepCycles = sleepFacet.getHeartRateCurve();
            for (Pair<Long,Double> dataPoint : sleepCycles){
                List<Object> sample = new ArrayList<Object>();
                sample.add(dataPoint.getFirst() / 1000.0);
                sample.add(dataPoint.getSecond());
                data.add(sample);
            }
        }
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "Beddit", Arrays.asList("heartRate"), data));
    }

    @Override
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 1, apiKey.getConnector().getDeviceNickname(), "heartRate", channelMappings);
    }
}