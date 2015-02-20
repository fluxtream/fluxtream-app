package org.fluxtream.connectors.fluxtream_capture;

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

/**
 * Created by candide on 18/02/15.
 */
@Component("fluxtreamObservation")
public class FluxtreamObservationFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField(ApiKey apiKey, AbstractFacet facet) {
        FluxtreamObservationFacet observationFacet = (FluxtreamObservationFacet)facet;
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> row = new ArrayList<Object>();
        row.add(observationFacet.start / 1000.0);
        row.add(observationFacet.value);
        row.add(observationFacet.comment);
        data.add(row);
        String observationName = "topic_" + observationFacet.topicId;

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "FluxtreamCapture", Arrays.asList(observationName, observationName + "._comment"), data));
    }

    @Override
    public void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> mappings) {}
}
