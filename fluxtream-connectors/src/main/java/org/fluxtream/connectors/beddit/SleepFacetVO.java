package org.fluxtream.connectors.beddit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;

import java.util.List;

/**
 * Created by justin on 11/30/14.
 */
public class SleepFacetVO extends AbstractTimedFacetVO<SleepFacet> {

    public double sleepTimeTarget;
    public double snoringAmount;
    public double restingHeartRate;
    public Double respirationRate;
    public Double timeToFallAsleep;
    public double totalSleepTime;
    public List<String> sleepTags;

    @Override
    protected void fromFacet(SleepFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        sleepTimeTarget = facet.sleepTimeTarget;
        snoringAmount = facet.snoringAmount;
        restingHeartRate = facet.restingHeartRate;
        if (facet.respirationRate != null)
            respirationRate = facet.respirationRate;
        if (facet.timeToFallAsleep != null)
            timeToFallAsleep = facet.timeToFallAsleep;
        totalSleepTime = facet.totalSleepTime;
        sleepTags = facet.getSleepTags();

    }
}
