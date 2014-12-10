package org.fluxtream.connectors.zeo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("zeoBedTime")
public class ZeoBedTimeFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField (final ApiKey apiKey, AbstractFacet facet) {
        DateTime startTimeJoda = new DateTime(facet.start,DateTimeZone.UTC);
        int stHour = startTimeJoda.getHourOfDay();
        int stMin = startTimeJoda.getMinuteOfHour();

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();
        // Creating the array [time, bedTime_in_hours] to insert in datastore
        record.add(((double)facet.start)/1000.0);
        record.add(((double)stHour)+((double)stMin)/60.0);
        data.add(record);

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey , "Zeo", Arrays.asList("bedTime"), data));
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 1, apiKey.getConnector().getDeviceNickname(), "bedTime", channelMappings);
    }
}
