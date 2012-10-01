package com.fluxtream.connectors.mymee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fluxtream.connectors.zeo.ZeoSleepStatsFacet;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("mymeeObservation")
public class MymeeObservationFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        MymeeObservationFacet observationFacet = (MymeeObservationFacet) facet;
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> row = new ArrayList<Object>();
        row.add(observationFacet.start/1000);
        row.add(observationFacet.amount);
        row.add(observationFacet.note);
        data.add(row);
        final String observationName = observationFacet.name;
        observationName.replaceAll("[^0-9a-zA-Z_]+", "_");
        bodyTrackHelper.uploadToBodyTrack(guestId , "Mymee", Arrays.asList(observationName,
                                                                           observationName + "._comment"),
                                          data);
    }

}
