package org.fluxtream.connectors.withings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.services.impl.BodyTrackHelper;
import org.fluxtream.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("withingsFatFreeMass")
public class WithingsFatFreeMassFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public void handleField ( final long guestId, AbstractFacet facet) {
        WithingsBodyScaleMeasureFacet measureFacet = (WithingsBodyScaleMeasureFacet) facet;
        if (measureFacet.fatFreeMass == 0)
            return;

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> record = new ArrayList<Object>();
        // Creating the array [time, bedTime_in_hours] to insert in datastore
        record.add(((double)facet.start)/1000.0);
        record.add(measureFacet.fatFreeMass);
        data.add(record);

        // TODO: check the status code in the BodyTrackUploadResult
        bodyTrackHelper.uploadToBodyTrack(guestId , "Withings", Arrays.asList("fatFreeMass"), data);
    }
}