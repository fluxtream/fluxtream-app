package org.fluxtream.connectors.runkeeper;

import java.util.Date;
import java.util.TimeZone;
import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.DurationModel;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class RunKeeperFitnessActivityFacetVO extends AbstractTimedFacetVO<RunKeeperFitnessActivityFacet> {

    public Integer averageHeartRate;
    public double total_distance;
    public Double total_climb;
    public String activityType;
    public Double totalCalories;

    @Override
    protected void fromFacet(final RunKeeperFitnessActivityFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        TimeZone timeZone = TimeZone.getTimeZone(facet.timeZone);
        this.startMinute = toMinuteOfDay(new Date(facet.start), timeZone);
        this.endMinute = toMinuteOfDay(new Date(facet.end), timeZone);
        this.totalCalories = facet.totalCalories;
        this.averageHeartRate = (facet.averageHeartRate!=null && facet.averageHeartRate>0)?facet.averageHeartRate:null;
        this.total_distance = facet.total_distance;
        this.duration = new DurationModel(facet.duration);
        this.total_climb = facet.total_climb;
        this.activityType = facet.type;
    }

}
