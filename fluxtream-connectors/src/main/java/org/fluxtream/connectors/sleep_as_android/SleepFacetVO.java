package org.fluxtream.connectors.sleep_as_android;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;

import java.util.List;

/**
 * Created by justin on 10/12/14.
 */
public class SleepFacetVO extends AbstractTimedFacetVO<SleepFacet> {

    public int cycles;
    public Double rating;
    public double ratioDeepSleep;
    public DurationModel snoringDuration;
    public double noiseLevel;
    public List<String> sleepTags;
    public String sleepComment;

    @Override
    protected void fromFacet(SleepFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.cycles = facet.cycles;
        if (facet.rating>0)
            this.rating = facet.rating;
        this.ratioDeepSleep = round(facet.ratioDeepSleep*100,0);
        if (facet.snoringSeconds>0)
            this.snoringDuration = new DurationModel(facet.snoringSeconds);
        this.noiseLevel = facet.noiseLevel;
        this.sleepTags = facet.getSleepTags();
        if (facet.sleepComment!=null)
            this.sleepComment = facet.sleepComment;
    }
}
