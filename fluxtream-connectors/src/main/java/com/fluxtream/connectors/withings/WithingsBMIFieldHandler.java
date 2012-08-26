package com.fluxtream.connectors.withings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fluxtream.connectors.bodymedia.BodymediaBurnFacet;
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
@Component("withingsBMI")
public class WithingsBMIFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        WithingsBodyScaleMeasureFacet weightFacet = (WithingsBodyScaleMeasureFacet) facet;
        if (weightFacet.height == 0) {
            return;
        }
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();
        record.add(facet.start/1000);
        record.add(bmi(weightFacet));
        data.add(record);
        bodyTrackHelper.uploadToBodyTrack(guestId, "Withings", Arrays.asList("bmi"), data);
    }

    private double bmi(final WithingsBodyScaleMeasureFacet weightFacet) {
        return weightFacet.weight/(weightFacet.height*weightFacet.height);
    }

}