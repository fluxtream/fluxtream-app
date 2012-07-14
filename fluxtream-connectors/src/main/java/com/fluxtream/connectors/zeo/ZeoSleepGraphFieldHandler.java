package com.fluxtream.connectors.zeo;

import java.util.HashMap;
import java.util.Map;
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
@Component("zeoSleepGraph")
public class ZeoSleepGraphFieldHandler implements FieldHandler {

    int timeIncrement = 300; // 5 minutes

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        ZeoSleepStatsFacet sleepStatsFacet = (ZeoSleepStatsFacet) facet;
        if (sleepStatsFacet.sleepGraph==null)
            return;
        Map<String,String> params = new HashMap<String,String>();
        createJsonBlockHeader(params);
        JSONArray dataArray = new JSONArray();
        populateDataArray(dataArray, sleepStatsFacet.sleepGraph, facet.start);
        params.put("data", dataArray.toString());
        bodyTrackHelper.uploadToBodyTrack(guestId , params);
    }

    private void createJsonBlockHeader(Map<String,String> params) {
        JSONArray channelNamesArray = new JSONArray();
        channelNamesArray.add("Sleep_Graph");
        params.put("dev_nickname", "Zeo");
        params.put("channel_names", channelNamesArray.toString());
        JSONObject channelSpecsObject = new JSONObject();
        JSONObject zeoTypeObject = new JSONObject();
        zeoTypeObject.accumulate("type", "zeo");
        channelSpecsObject.accumulate("Sleep_Graph", zeoTypeObject);
        params.put("channel_specs", channelSpecsObject.toString());
    }

    private void populateDataArray(final JSONArray dataArray, final String sleepGraph, long start) {
        int graphSize = sleepGraph.length();
        start /= 1000;
        for (int i=0; i<graphSize; i++) {
            addSleepGraphColumn(dataArray, sleepGraph, start+i*timeIncrement, i);
        }
    }

    private void addSleepGraphColumn(final JSONArray dataArray, final String sleepGraph, final long time, final int i) {
        JSONArray sleepGraphColumnArray = new JSONArray();
        sleepGraphColumnArray.add(time);
        sleepGraphColumnArray.add(Integer.valueOf(""+sleepGraph.charAt(i)));
        dataArray.add(sleepGraphColumnArray);
    }

    @Override
    public String getBodytrackChannelName() {
        return "Sleep_Graph";
    }

}
