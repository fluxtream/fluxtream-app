package com.fluxtream.connectors.bodymedia;

import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("bodyMediaBurnJson")
public class BodyMediaBurnFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, final String user_id, final String host, AbstractFacet facet) {
    }

    @Override
    public String getBodytrackChannelName() {
        return "Burn_Graph";
    }

}
