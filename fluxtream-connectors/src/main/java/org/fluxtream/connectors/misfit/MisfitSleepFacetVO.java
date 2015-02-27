package org.fluxtream.connectors.misfit;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.DurationModel;

/**
 * Created by candide on 24/02/15.
 */
public class MisfitSleepFacetVO extends AbstractTimedFacetVO<MisfitSleepFacet> {

    DurationModel duration;

    @Override
    protected void fromFacet(MisfitSleepFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.date = facet.date;
        this.duration = new DurationModel((int)((facet.end - facet.start)/1000));
    }
}
