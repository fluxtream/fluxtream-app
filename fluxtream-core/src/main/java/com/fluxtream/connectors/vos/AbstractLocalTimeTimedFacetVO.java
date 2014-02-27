package com.fluxtream.connectors.vos;

import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import com.fluxtream.domain.GuestSettings;

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
