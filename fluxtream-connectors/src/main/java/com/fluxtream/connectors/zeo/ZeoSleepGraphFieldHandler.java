package com.fluxtream.connectors.zeo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.services.impl.FieldHandler;
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
        int graphSize = sleepStatsFacet.sleepGraph.length();
        List<List<Object>> data = new ArrayList<List<Object>>();
        for (int i=0; i<graphSize; i++) {
            addSleepGraphColumn(data, sleepStatsFacet.sleepGraph, facet.start/1000+i*timeIncrement, i);
        }
        bodyTrackHelper.uploadToBodyTrack(guestId , "Zeo", Arrays.asList("sleepGraph"), data);
    }

    private void addSleepGraphColumn(final List<List<Object>> data, final String sleepGraph, final long time, final int i) {
        List<Object> record = new ArrayList<Object>();
        record.add(time);
        record.add(Integer.valueOf(""+sleepGraph.charAt(i)));
        data.add(record);
    }

}
