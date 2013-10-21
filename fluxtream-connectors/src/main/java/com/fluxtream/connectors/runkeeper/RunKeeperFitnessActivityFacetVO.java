package com.fluxtream.connectors.runkeeper;

import java.util.Date;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.DurationModel;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RunKeeperFitnessActivityFacetVO extends AbstractTimedFacetVO<RunKeeperFitnessActivityFacet> {

    public int averageHeartRate;
    public double total_distance;
    public double total_climb;
    public String activityType;

    @Override
    protected void fromFacet(final RunKeeperFitnessActivityFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        TimeZone timeZone = TimeZone.getTimeZone(facet.timeZone);
        this.startMinute = toMinuteOfDay(new Date(facet.start), timeZone);
        this.endMinute = toMinuteOfDay(new Date(facet.end), timeZone);
        averageHeartRate = facet.averageHeartRate;
        this.total_distance = facet.total_distance;
        this.duration = new DurationModel(facet.duration);
        this.total_climb = facet.total_climb;
        this.activityType = facet.type;
    }

}
