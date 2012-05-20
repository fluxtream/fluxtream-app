package com.fluxtream.services.impl.converters;

import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface Converter {

    String convert(AbstractFacet facet);

    String getBodytrackChannelName();

    boolean returnsFullJsonBlock();

}