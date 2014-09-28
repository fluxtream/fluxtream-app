package org.fluxtream.core.services;

import java.util.Map;
import java.util.SortedSet;
import org.fluxtream.core.OutsideTimeBoundariesException;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.TagFilter;
import org.jetbrains.annotations.Nullable;

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
     * <code>guestId</code> and {@link SimpleTimeInterval}.
     */
    SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException, OutsideTimeBoundariesException;

    /**
     * <p>
     * Returns a {@link SortedSet} of {@link Photo} objects for the given <code>guestId</code> and within the given
     * {@link SimpleTimeInterval} which belong to the {@link Connector} {@link ObjectType} specified by the given
     * <code>connectorPrettyName</code> and <code>objectTypeName</code>.
     * </p>
     * <p>
     * If the given <code>objectTypeName</code>
     * does not specify an existing {@link ObjectType} for the {@link Connector}, then this method returns photos from
     * from all {@link ObjectType}s which are of {@link ObjectType#isImageType() image type}.
     * </p>
     * <p>
     * The set of returned photos may optionally be filtered by the given {@link TagFilter}.
     * </p>
     */
    SortedSet<Photo> getPhotos(long guestId,
                               TimeInterval timeInterval,
                               String connectorPrettyName,
                               String objectTypeName,
                               @Nullable TagFilter tagFilter) throws ClassNotFoundException, IllegalAccessException, InstantiationException, OutsideTimeBoundariesException;

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
     * <p>
     * The set of returned photos may optionally be filtered by the given {@link TagFilter}.
     * </p>
     */
    SortedSet<Photo> getPhotos(long guestId,
                               long timeInMillis,
                               String connectorPrettyName,
                               String objectTypeName,
                               int desiredCount,
                               boolean isGetPhotosBeforeTime,
                               @Nullable TagFilter tagFilter) throws InstantiationException, IllegalAccessException, ClassNotFoundException, OutsideTimeBoundariesException;

    /**
     * Returns a {@link Map} of photo channels (a {@link String} which is of the form {connector_pretty_name}.{object_name})
     * mapped to a {@link SimpleTimeInterval} which specifies the time range for that channel.  May return an empty
     * {@link Map}, but guaranteed to not return <code>null</code>.  Note that the {@link SimpleTimeInterval} for a channel
     * may be <code>null</code>, for example if the channel is a photo channel, but it currently contains no photos.
     */
    Map<String, TimeInterval> getPhotoChannelTimeRanges(long guestId, final CoachingBuddy coachee);
}