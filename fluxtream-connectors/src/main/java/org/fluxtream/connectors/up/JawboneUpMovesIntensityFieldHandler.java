package org.fluxtream.connectors.up;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.services.impl.BodyTrackHelper;
import org.fluxtream.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 12/02/14
 * Time: 10:25
 */
@Component("upMovesIntensity")
public class JawboneUpMovesIntensityFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField(final long guestId, final AbstractFacet facet) {
        JawboneUpMovesFacet movesFacet = (JawboneUpMovesFacet) facet;
        if (movesFacet.intensityStorage==null|| StringUtils.isEmpty(movesFacet.intensityStorage))
            return;
        JSONArray intensityJson = JSONArray.fromObject(movesFacet.intensityStorage);
        List<List<Object>> data = new ArrayList<List<Object>>();
        for(int i=0; i<intensityJson.size(); i++) {
            JSONArray record = intensityJson.getJSONArray(i);
            final long when = record.getLong(0);
            final double intensity = record.getDouble(1);
            List<Object> intensityRecord = new ArrayList<Object>();
            intensityRecord.add(when);
            intensityRecord.add(intensity);
            data.add(intensityRecord);
        }
        final List<String> channelNames = Arrays.asList("intensity");

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId, "Jawbone_UP", channelNames, data);
    }
}
