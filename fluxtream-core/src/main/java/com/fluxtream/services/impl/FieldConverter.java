package com.fluxtream.services.impl;

import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface FieldConverter {

    String convertField(final long guestId, AbstractFacet facet);

    String getBodytrackChannelName();

}