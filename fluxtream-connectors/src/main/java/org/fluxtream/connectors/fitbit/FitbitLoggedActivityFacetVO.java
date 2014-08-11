package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.joda.time.LocalDateTime;

public class FitbitLoggedActivityFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitLoggedActivityFacet> {

	int steps;
	int caloriesOut;
	
	@Override
	public void fromFacet(FitbitLoggedActivityFacet facet, TimeInterval timeInterval,
			GuestSettings settings) {
		this.steps = facet.steps;
		caloriesOut = facet.calories;
		description = facet.fullTextDescription;
        LocalDateTime startTime = new LocalDateTime(facet.start);
        LocalDateTime endTime = new LocalDateTime(facet.end);
	}

}
