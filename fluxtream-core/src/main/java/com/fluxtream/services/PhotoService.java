package com.fluxtream.services;

import java.util.Map;
import java.util.SortedSet;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface PhotoService {

    interface Photo extends Comparable<Photo> {
        AbstractPhotoFacetVO getAbstractPhotoFacetVO();

        Connector getConnector();

        ObjectType getObjectType();
    }

    SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval, String connectorPrettyName, String objectTypeName) throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    Map<String, TimeInterval> getPhotoChannelTimeRanges(long guestId);
}