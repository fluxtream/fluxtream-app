package com.fluxtream.services;

import java.util.Map;
import java.util.SortedSet;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.CoachingBuddy;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public interface PhotoService {

    String ALL_DEVICES_NAME = "All";

    String DEFAULT_PHOTOS_CHANNEL_NAME = "photo";

    interface Photo extends Comparable<Photo> {

        AbstractPhotoFacetVO getAbstractPhotoFacetVO();
        Connector getConnector();

        ObjectType getObjectType();

    }
    SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval, String connectorPrettyName, String objectTypeName) throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    SortedSet<Photo> getPhotos(long guestId, long timeInMillis, String connectorPrettyName, String objectTypeName, int desiredCount, boolean isGetPhotosBeforeTime) throws InstantiationException, IllegalAccessException, ClassNotFoundException;

    Map<String, TimeInterval> getPhotoChannelTimeRanges(long guestId, final CoachingBuddy coachee);
}