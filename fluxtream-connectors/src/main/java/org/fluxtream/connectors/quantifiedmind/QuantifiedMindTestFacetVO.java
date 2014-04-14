package org.fluxtream.connectors.quantifiedmind;

import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.vos.AbstractInstantFacetVO;
import org.fluxtream.core.domain.GuestSettings;

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
        this.test_name = facet.test_name;
        this.result_name = facet.result_name;
        this.session_timestamp = facet.session_timestamp;
        this.result_value = round(facet.result_value);
    }

    double round(double v) {
        return (double) Math.round(v * 100) / 100;
    }

}
