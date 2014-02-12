package com.fluxtream.connectors.up;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 12/02/14
 * Time: 10:25
 */
@Component("upSleepPhases")
public class JawboneUpSleepPhasesFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField(final long guestId, final AbstractFacet facet) {
        JawboneUpSleepFacet sleepFacet = (JawboneUpSleepFacet) facet;
        if (sleepFacet.phasesStorage==null|| StringUtils.isEmpty(sleepFacet.phasesStorage))
            return;
        JSONArray phasesJson = JSONArray.fromObject(sleepFacet.phasesStorage);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<phasesJson.size(); i++) {
            JSONArray record = phasesJson.getJSONArray(i);
            final long when = record.getLong(0);
            final int phase = record.getInt(1);
            List<Object> intensityRecord = new ArrayList<Object>();
            intensityRecord.add(when);
            intensityRecord.add(phase);
            data.add(intensityRecord);
        }
        final List<String> channelNames = Arrays.asList("phases");

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId, "Jawbone_UP", channelNames, data);
    }
}
