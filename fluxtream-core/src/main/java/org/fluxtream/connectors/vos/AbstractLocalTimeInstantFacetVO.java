package org.fluxtream.connectors.vos;

import org.fluxtream.OutsideTimeBoundariesException;
import org.fluxtream.TimeInterval;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.fluxtream.domain.GuestSettings;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public abstract class AbstractLocalTimeInstantFacetVO<T extends AbstractLocalTimeFacet> extends
                                             AbstractFacetVO<T> implements Comparable<AbstractInstantFacetVO<T>> {

    public transient long start;
    public int startMinute;
    public TimeOfDayVO startTime;
    public boolean localTime;

    @Override
    public void extractValues(T facet, TimeInterval timeInterval, GuestSettings settings) throws OutsideTimeBoundariesException {
        super.extractValues(facet, timeInterval, settings);
        this.date = facet.date;
        this.start = facet.start;
        this.startTime = new TimeOfDayVO(startMinute, settings.distanceMeasureUnit == GuestSettings.DistanceMeasureUnit.MILES_YARDS);
        localTime = true;
    }

    @Override
    public int compareTo(final AbstractInstantFacetVO<T> other) {
        return this.start > other.start ? 1 : -1;
    }

}
