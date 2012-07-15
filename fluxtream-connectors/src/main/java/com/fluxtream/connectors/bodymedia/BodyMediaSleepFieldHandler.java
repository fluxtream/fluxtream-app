package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fluxtream.connectors.zeo.ZeoSleepStatsFacet;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("bodyMediaSleepJson")
public class BodyMediaSleepFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        //BodymediaSleepFacet sleepFacet = (BodymediaSleepFacet) facet;
        //if (sleepFacet.json == null) {
        //    return;
        //}
        //JSONArray sleepJson = JSONArray.fromObject(sleepFacet.json);
        //List<List<Object>> data = new ArrayList<List<Object>>();
        //for(int i=0; i<sleepJson.size(); i++) {
        //    JSONObject jsonRecord = sleepJson.getJSONObject(i);
        //    final int minuteIndex = jsonRecord.getInt("minuteIndex");
        //    final int duration = jsonRecord.getInt("duration");
        //    long when = (facet.start/1000) + minuteIndex*60;
        //    final String state = jsonRecord.getString("state");
        //    List<Object> record = new ArrayList<Object>();
        //    record.add(when);
        //    data.add(record);
        //}
        //final List<String> channelNames = Arrays.asList("lying", "sleeping");
        //bodyTrackHelper.uploadToBodyTrack(guestId, "BodyMedia", channelNames, data);
    }

}
