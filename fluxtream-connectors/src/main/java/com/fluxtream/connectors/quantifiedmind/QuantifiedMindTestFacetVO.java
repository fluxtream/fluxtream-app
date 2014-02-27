package com.fluxtream.connectors.quantifiedmind;

import java.util.Date;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.GuestSettings;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class QuantifiedMindTestFacetVO extends AbstractInstantFacetVO<QuantifiedMindTestFacet> {

    public String test_name;
    public String result_name;
    public long session_timestamp;
    public double result_value;

    @Override
    protected void fromFacet(final QuantifiedMindTestFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getTimeZone(facet.start));
        this.start = facet.start;
        this.test_name = facet.test_name;
        this.result_name = facet.result_name;
        this.session_timestamp = facet.session_timestamp;
        this.result_value = round(facet.result_value);
    }

    double round(double v) {
        return (double) Math.round(v * 100) / 100;
    }

}
