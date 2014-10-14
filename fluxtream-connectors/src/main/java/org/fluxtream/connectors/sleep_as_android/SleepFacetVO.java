package org.fluxtream.connectors.sleep_as_android;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.connectors.vos.AbstractTimedFacetVO;
import org.fluxtream.core.domain.GuestSettings;

/**
 * Created by justin on 10/12/14.
 */
public class SleepFacetVO extends AbstractTimedFacetVO<SleepFacet> {

    public int cycles;

    @Override
    protected void fromFacet(SleepFacet facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        this.cycles = facet.cycles;
    }
}
