package org.fluxtream.connectors.bodymedia;

import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.connectors.vos.AllDayVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;

public class BodymediaSleepFacetVO extends AbstractTimedFacetVO<BodymediaSleepFacet> implements AllDayVO {

    double efficiency;
    DurationModel totalLying;
    DurationModel totalSleeping;
//    String sleepJson;

    @Override
    protected void fromFacet(final BodymediaSleepFacet facet, final TimeInterval timeInterval, final GuestSettings settings) {
        this.date = facet.date;
        this.efficiency = facet.efficiency;
        this.totalSleeping = new DurationModel(facet.totalSleeping*60);
        this.totalLying = new DurationModel(facet.totalLying*60);
//        this.sleepJson = facet.json;
    }

    @Override
    public boolean allDay() {
        return true;
    }

}
