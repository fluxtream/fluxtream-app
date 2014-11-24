package org.fluxtream.connectors.beddit;

import com.google.gdata.util.common.base.Pair;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.services.impl.FieldHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("bedditSleepCycle")
public class BedditSleepCycleFieldHandler implements FieldHandler {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Override
    public List<BodyTrackHelper.BodyTrackUploadResult> handleField(long guestId, AbstractFacet facet) {
        SleepFacet sleepFacet = (SleepFacet) facet;
        List<Pair<Long,Double>> sleepCycles = sleepFacet.getSleepCycles();
        List<List<Object>> data = new ArrayList<List<Object>>();
        for (Pair<Long,Double> dataPoint : sleepCycles){
            List<Object> sample = new ArrayList<Object>();
            sample.add(dataPoint.getFirst() / 1000.0);
            sample.add(dataPoint.getSecond());
            data.add(sample);
        }
        final List<String> channelNames = Arrays.asList("sleepCycles");

        return Arrays.asList(bodyTrackHelper.uploadToBodyTrack(guestId, "beddit", channelNames, data));
    }
}