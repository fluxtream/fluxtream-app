package com.fluxtream.connectors.bodymedia;

import java.util.HashMap;
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
    public void handleField ( final long guestId, final String user_id, final String host, AbstractFacet facet) {
    }

    @Override
    public String getBodytrackChannelName() {
        return "Sleep_Graph";
    }

}
