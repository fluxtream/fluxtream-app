package com.fluxtream.services;

import java.util.List;
import java.util.Map;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.Guest;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface PhotoService {

    boolean hasPhotos(Guest guest);

    List<AbstractInstantFacetVO<AbstractFacet>> getPhotos(Guest guest, TimeInterval timeInterval)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    Map<String, TimeInterval> getPhotoChannelTimeRanges(long guestId);
}