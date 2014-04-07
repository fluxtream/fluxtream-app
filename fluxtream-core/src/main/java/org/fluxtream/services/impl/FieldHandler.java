package org.fluxtream.services.impl;

import java.util.List;
import org.fluxtream.domain.AbstractFacet;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public interface FieldHandler {

    List<BodyTrackHelper.BodyTrackUploadResult> handleField (final long guestId, AbstractFacet facet );

}