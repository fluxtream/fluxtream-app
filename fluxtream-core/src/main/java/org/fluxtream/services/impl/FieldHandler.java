package org.fluxtream.services.impl;

import org.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface FieldHandler {

    void handleField (final long guestId, AbstractFacet facet );

}