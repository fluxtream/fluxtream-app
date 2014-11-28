package org.fluxtream.connectors.zeo;

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
import org.joda.time.DateTimeZone;
import org.joda.time.DateTime;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("zeoSleepGraph")
public class ZeoSleepGraphFieldHandler implements FieldHandler {

    int timeIncrement = 300; // 5 minutes

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField (final ApiKey apiKey, AbstractFacet facet) {
        ZeoSleepStatsFacet sleepStatsFacet = (ZeoSleepStatsFacet) facet;
        if (sleepStatsFacet.sleepGraph==null)
            return Arrays.asList();
        int graphSize = sleepStatsFacet.sleepGraph.length();
        // To be consistent with the Zeo web site, round the start time down to the previous
        // 5-minute boundary to compute the graph start time to use.
        DateTime startTimeJoda = new DateTime(facet.start,DateTimeZone.UTC);
        int stMin = startTimeJoda.getMinuteOfHour();
        // Compute number of minutes we need to subtract from startTime to get graph start time
        int gtMinSub = stMin%5;
        // For some reason it looks like if the sleep time is at an even 5-minute boundary
        // they are starting the graph 5 minutes before that
        if(gtMinSub == 0) {
            gtMinSub = 5;
        }
        // Subtract seconds/milliseconds of startTime + gtMinSub minutes in milliseconds
        // from start to get graph start time
        long graphStartTime = facet.start - (startTimeJoda.getMillisOfSecond()+startTimeJoda.getSecondOfMinute()*1000+gtMinSub*60000);


        List<List<Object>> data = new ArrayList<List<Object>>();
        for (int i=0; i<graphSize; i++) {
            addSleepGraphColumn(data, sleepStatsFacet.sleepGraph, graphStartTime/1000+i*timeIncrement, i);
        }

        // TODO: check the status code in the BodyTrackUploadResult
        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(apiKey , "Zeo", Arrays.asList("Sleep_Graph"), data));
    }

    private void addSleepGraphColumn(final List<List<Object>> data, final String sleepGraph, final long time, final int i) {
        List<Object> record = new ArrayList<Object>();
        record.add(time);
        record.add(5-Integer.valueOf(""+sleepGraph.charAt(i)));
        data.add(record);
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping.addToDeclaredMappings(apiKey, 1, apiKey.getConnector().getDeviceNickname(), "Sleep_Graph", channelMappings);
    }

}
