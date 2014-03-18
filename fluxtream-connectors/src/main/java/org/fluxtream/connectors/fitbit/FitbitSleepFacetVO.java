package org.fluxtream.connectors.fitbit;

import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.vos.AbstractLocalTimeTimedFacetVO;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.DurationModel;

public class FitbitSleepFacetVO extends AbstractLocalTimeTimedFacetVO<FitbitSleepFacet> {

	public DurationModel minutesAsleep;
	public DurationModel minutesAwake;
	public DurationModel minutesToFallAsleep;

	@Override
	public void fromFacet(FitbitSleepFacet facet, TimeInterval timeInterval, GuestSettings settings) {
		minutesAsleep = new DurationModel(facet.minutesAsleep*60);
		minutesAwake = new DurationModel(facet.minutesAwake*60);
		minutesToFallAsleep = new DurationModel(facet.minutesToFallAsleep*60);
        date = facet.date;
	}

}
