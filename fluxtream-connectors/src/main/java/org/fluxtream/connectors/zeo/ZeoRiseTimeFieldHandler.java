package org.fluxtream.connectors.zeo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.services.impl.BodyTrackHelper;
import org.fluxtream.services.impl.FieldHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("zeoRiseTime")
public class ZeoRiseTimeFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        ZeoSleepStatsFacet sleepStatsFacet = (ZeoSleepStatsFacet) facet;
        DateTime riseTimeJoda = new DateTime(facet.end,DateTimeZone.UTC);
        int rtHour = riseTimeJoda.getHourOfDay();
        int rtMin = riseTimeJoda.getMinuteOfHour();

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();
        // Creating the array [time, riseTime_in_hours] to insert in datastore
        record.add(((double)facet.end)/1000.0);
        record.add(((double)rtHour)+((double)rtMin)/60.0);
        data.add(record);

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId, "Zeo", Arrays.asList("riseTime"), data);
    }
}
