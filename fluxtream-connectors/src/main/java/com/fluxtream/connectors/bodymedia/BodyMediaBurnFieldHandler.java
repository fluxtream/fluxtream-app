package com.fluxtream.connectors.bodymedia;

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
 * @author Prasanth Somasundar
 */
@Component("bodyMediaBurnJson")
public class BodyMediaBurnFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        BodymediaBurnFacet burnFacet = (BodymediaBurnFacet) facet;
        if(burnFacet.getJson() == null)
            return;
        Map<String, String> params = populateParams();
        JSONArray data = parseData(burnFacet.getJson(), burnFacet.start);
        params.put("data", data.toString());
        bodyTrackHelper.uploadToBodyTrack(guestId, params);
    }

    private Map<String, String> populateParams() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("dev_nickname", "Bodymedia");

        JSONArray channelNamesArray = new JSONArray();
        channelNamesArray.add("Calories_Burned");
        params.put("chanel_names", channelNamesArray.toString());

        JSONObject channelSpecsObject;
        channelSpecsObject = new JSONObject();
        JSONObject bodymediaTypeObject = new JSONObject();
        bodymediaTypeObject.accumulate("type", "Float");
        bodymediaTypeObject.accumulate("units", "calories");
        channelSpecsObject.accumulate("Burn_Graph", bodymediaTypeObject);
        params.put("channel_specs", channelSpecsObject.toString());
        return params;
    }

    private JSONArray parseData(final String jsonString, final long start) {
        JSONArray json = JSONArray.fromObject(jsonString);
        JSONArray data = new JSONArray();
        data.add(parseJsonData(json, start));
        return data;
    }

    private JSONArray parseJsonData(final JSONArray jsonArray, long start) {
        JSONArray json = new JSONArray();
        for(int i = 0; i < jsonArray.size(); i++)
        {
            JSONObject burn = (JSONObject) jsonArray.get(i);
            JSONArray day = new JSONArray();
            day.add((start/100) + i*60);
            day.add(burn.get("cals"));
            json.add(day);
        }
        return json;
    }

    @Override
    public String getBodytrackChannelName() {
        return "Burn_Graph";
    }

}