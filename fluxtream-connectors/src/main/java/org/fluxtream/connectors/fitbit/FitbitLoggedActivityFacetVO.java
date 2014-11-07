package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;

public class FitbitLoggedActivityFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitLoggedActivityFacet> {

	public int steps;
    public int caloriesOut;
    public double distance;
    public String name;
	
	@Override
	public void fromFacet(FitbitLoggedActivityFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		this.steps = facet.steps;
		caloriesOut = facet.calories;
        this.description = facet.name;
        this.distance = facet.distance;
        this.name = facet.name;
	}

}
