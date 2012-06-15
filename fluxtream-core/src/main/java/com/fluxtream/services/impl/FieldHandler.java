package com.fluxtream.services.impl;

import com.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface FieldHandler {

    void handleField (final long guestId, final String user_id, final String host, AbstractFacet facet );

    String getBodytrackChannelName();

}