package com.fluxtream.connectors.zeo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("zeoBedTime")
public class ZeoRiseTimeFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        ZeoSleepStatsFacet sleepStatsFacet = (ZeoSleepStatsFacet) facet;
        DateTime startTimeJoda = new DateTime(facet.start,DateTimeZone.UTC);
        int stHour = startTimeJoda.getHourOfDay();
        int stMin = startTimeJoda.getMinuteOfHour();

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();
        // Want [time, bedTime_in_hours]
        record.add(facet.start);
        record.add(((double)stHour)+((double)stMin)/60.0);

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId, "Zeo", Arrays.asList("bedTime"), data);
    }
}
