package org.fluxtream.connectors.runkeeper;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;

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
        this.totalCalories = facet.totalCalories;
        this.averageHeartRate = (facet.averageHeartRate!=null && facet.averageHeartRate>0)?facet.averageHeartRate:null;
        this.total_distance = facet.total_distance;
        this.duration = new DurationModel(facet.duration);
        this.total_climb = facet.total_climb;
        this.activityType = facet.type;
    }

}
