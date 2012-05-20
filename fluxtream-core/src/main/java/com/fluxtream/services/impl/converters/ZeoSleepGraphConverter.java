package com.fluxtream.services.impl.converters;

import java.util.Date;
import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class ZeoSleepGraphConverter  implements Converter {

    @Override
    public String convert(AbstractFacet facet) {
        return null;
    }

    @Override
    public String getBodytrackChannelName() {
        return "Sleep_Graph";
    }

    @Override
    public boolean returnsFullJsonBlock() {
        return true;
    }

}
