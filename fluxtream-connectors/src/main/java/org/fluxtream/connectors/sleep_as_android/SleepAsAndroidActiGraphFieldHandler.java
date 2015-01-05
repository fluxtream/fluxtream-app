package org.fluxtream.connectors.sleep_as_android;

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

@Component("sleepAsAndroidActiGraph")
public class SleepAsAndroidActiGraphFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField(ApiKey apiKey, AbstractFacet facet) {
        SleepFacet sleepFacet = (SleepFacet) facet;
        List<Double> actiGraph = sleepFacet.getActiGraph();
        double start = sleepFacet.start / 1000.0;
        double end = sleepFacet.end / 1000.0;
        double deltaPerSample = (end - start) / actiGraph.size();
        List<List<Object>> data = new ArrayList<List<Object>>();
        for (int i = 0; i < actiGraph.size(); i++){
            List<Object> sample = new ArrayList<Object>();
            sample.add(start + deltaPerSample * i);
            sample.add(actiGraph.get(i));
            data.add(sample);
        }
        final List<String> channelNames = Arrays.asList("actiGraph");

        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "Sleep_As_Android", channelNames, data));
    }

    @Override
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 1, apiKey.getConnector().getDeviceNickname(), "actiGraph", channelMappings);
    }
}
