package org.fluxtream.core.services.impl;

import org.fluxtream.core.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface FieldConverter {

    String convertField(final long guestId, AbstractFacet facet);

    String getBodytrackChannelName();

}