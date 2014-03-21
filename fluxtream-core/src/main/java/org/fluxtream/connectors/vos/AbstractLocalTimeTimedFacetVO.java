package org.fluxtream.connectors.vos;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.fluxtream.domain.GuestSettings;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public abstract class AbstractLocalTimeTimedFacetVO<T extends AbstractLocalTimeFacet> extends AbstractLocalTimeInstantFacetVO<T> {

    public int endMinute;
    public TimeOfDayVO endTime;

    @Override
    public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings)
            throws OutsideTimeBoundariesException
    {
        super.extractValues(facet, timeInterval, settings);
        this.endTime = new TimeOfDayVO(endMinute, settings.distanceMeasureUnit == GuestSettings.DistanceMeasureUnit.MILES_YARDS);
    }
}
