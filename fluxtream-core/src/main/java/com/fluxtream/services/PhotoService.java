package com.fluxtream.services;

import java.util.Map;
import java.util.SortedSet;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.ApiKey;
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

    /**
     * Gets all {@link Photo}s from all {@link Connector}s having image {@link ObjectType}s for the given
     * <code>guestId</code> and {@link TimeInterval}.
     */
    SortedSet<Photo> getPhotos(ApiKey apiKey, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    /**
     * <p>
     * Returns a {@link SortedSet} of {@link Photo} objects for the given <code>guestId</code> and within the given
     * {@link TimeInterval} which belong to the {@link Connector} {@link ObjectType} specified by the given
     * <code>connectorPrettyName</code> and <code>objectTypeName</code>.
     * </p>
     * <p>
     * If the given <code>objectTypeName</code>
     * does not specify an existing {@link ObjectType} for the {@link Connector}, then this method returns photos from
     * from all {@link ObjectType}s which are of {@link ObjectType#isImageType() image type}.
     * </p>
     */
    SortedSet<Photo> getPhotos(ApiKey apiKey, TimeInterval timeInterval, String connectorPrettyName, String objectTypeName) throws ClassNotFoundException, IllegalAccessException, InstantiationException;

    /**
     * <p>
     * Returns a {@link SortedSet} of up to <code>desiredCount</code> {@link Photo} objects either before or after
     * <code>timeInMillis</code> for the given <code>guestId</code> which belong to the {@link Connector}
     * {@link ObjectType} specified by the given <code>connectorPrettyName</code> and <code>objectTypeName</code>.  If
     * the given <code>objectTypeName</code> does not specify an existing {@link ObjectType} for the {@link Connector},
     * then this method returns photos from from all {@link ObjectType}s which are of {@link ObjectType#isImageType()
     * image type}.
     * </p>
     * <p>
     * If the given <code>objectTypeName</code>
     * does not specify an existing {@link ObjectType} for the {@link Connector}, then this method returns photos from
     * from all {@link ObjectType}s which are of {@link ObjectType#isImageType() image type}.
     * </p>
     */
    SortedSet<Photo> getPhotos(ApiKey apiKey, long timeInMillis, String connectorPrettyName, String objectTypeName, int desiredCount, boolean isGetPhotosBeforeTime) throws InstantiationException, IllegalAccessException, ClassNotFoundException;

    /**
     * Returns a {@link Map} of photo channels (a {@link String} which is of the form {connector_pretty_name}.{object_name})
     * mapped to a {@link TimeInterval} which specifies the time range for that channel.  May return an empty
     * {@link Map}, but guaranteed to not return <code>null</code>.  Note that the {@link TimeInterval} for a channel
     * may be <code>null</code>, for example if the channel is a photo channel, but it currently contains no photos.
     */
    Map<String, TimeInterval> getPhotoChannelTimeRanges(ApiKey apiKey, final CoachingBuddy coachee);
}