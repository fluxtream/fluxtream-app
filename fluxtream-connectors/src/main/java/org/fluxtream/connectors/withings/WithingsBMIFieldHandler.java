package org.fluxtream.connectors.withings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("withingsBMI")
public class WithingsBMIFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField (final ApiKey apiKey, AbstractFacet facet) {
        WithingsBodyScaleMeasureFacet weightFacet = (WithingsBodyScaleMeasureFacet) facet;
        if (weightFacet.height == 0) {
            return Arrays.asList();
        }
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();
        record.add(facet.start/1000);
        record.add(bmi(weightFacet));
        data.add(record);

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey, "Withings", Arrays.asList("bmi"), data));
    }

    private double bmi(final WithingsBodyScaleMeasureFacet weightFacet) {
        return weightFacet.weight/(weightFacet.height*weightFacet.height);
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 1, apiKey.getConnector().getDeviceNickname(), "bmi", channelMappings);
    }

}