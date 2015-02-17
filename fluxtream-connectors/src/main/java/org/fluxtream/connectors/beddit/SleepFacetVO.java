package org.fluxtream.connectors.beddit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;

import java.util.List;

/**
 * Created by justin on 11/30/14.
 */
public class SleepFacetVO extends AbstractTimedFacetVO<SleepFacet> {

    public DurationModel sleepTimeTarget;
    public DurationModel snoringDuration;
    public Double restingHeartRate;
    public Double respirationRate;
    public DurationModel timeToFallAsleep;
    public DurationModel totalSleepTime;
    public List<String> sleepTags;

    @Override
    protected void fromFacet(SleepFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        if (facet.sleepTimeTarget!=0)
            sleepTimeTarget = new DurationModel((int)facet.sleepTimeTarget);
        if (facet.snoringAmount>0)
           snoringDuration = new DurationModel((int)facet.snoringAmount);
        if (facet.restingHeartRate>0 && facet.restingHeartRate>0)
            restingHeartRate = round(facet.restingHeartRate, 2);
        if (facet.respirationRate != null && facet.respirationRate>0)
            respirationRate = round(facet.respirationRate, 2);
        if (facet.timeToFallAsleep != null)
            timeToFallAsleep = new DurationModel(facet.timeToFallAsleep.intValue());
        this.date = facet.date;
        totalSleepTime = new DurationModel((int)facet.totalSleepTime);
        sleepTags = facet.getSleepTags();
    }
}
