package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.PhotoService;
import com.fluxtream.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Service
@Component
public class PhotoServiceImpl implements PhotoService {

    @Autowired
    SettingsService settingsService;

    @Autowired
    private ApiDataService apiDataService;

    @Autowired
    GuestService guestService;

    private static interface PhotoFacetFinderStrategy {
        List<AbstractFacet> find(final Connector connector, final ObjectType objectType);
    }

    /**
     * Gets all photos from all connectors having image {@link ObjectType}s.
     */
    @Override
    public SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return getPhotos(guestId, timeInterval, ALL_DEVICES_NAME, DEFAULT_PHOTOS_CHANNEL_NAME);
    }


    @Override
    public SortedSet<Photo> getPhotos(final long guestId,
                                       final TimeInterval timeInterval,
                                       final String connectorPrettyName,
                                       final String objectTypeName)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return getPhotos(guestId,
                         timeInterval,
                         connectorPrettyName,
                         objectTypeName,
                         new PhotoFacetFinderStrategy() {
                             public List<AbstractFacet> find(final Connector connector, final ObjectType objectType) {
                                 return apiDataService.getApiDataFacets(guestId, connector, objectType, timeInterval);
                             }
                         });
    }

    @Override
    public SortedSet<Photo> getPhotos(final long guestId,
                                      final long timeInMillis,
                                      final String connectorPrettyName,
                                      final String objectTypeName,
                                      final int desiredCount,
                                      final boolean isGetPhotosBeforeTime)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        // make sure the count is >= 1
        final int cleanedDesiredCount = Math.max(1, desiredCount);

        final SortedSet<Photo> photos = getPhotos(guestId, null, connectorPrettyName, objectTypeName, new PhotoFacetFinderStrategy() {
            public List<AbstractFacet> find(final Connector connector, final ObjectType objectType) {
                if (isGetPhotosBeforeTime) {
                    return apiDataService.getApiDataFacetsBefore(guestId, connector, objectType, timeInMillis, cleanedDesiredCount);
                }
                else {
                    return apiDataService.getApiDataFacetsAfter(guestId, connector, objectType, timeInMillis, cleanedDesiredCount);
                }
            }
        });

        // Make sure we don't return more than requested (which may happen if we're merging from multiple photo channels,
        // which can happen for the All.photos device/channel).
        if (photos.size() > cleanedDesiredCount) {

            // first convert to a list for easier extraction
            List<Photo> photosList = new ArrayList<Photo>(photos);

            final SortedSet<Photo> photosSubset = new TreeSet<Photo>();
            if (isGetPhotosBeforeTime) {
                // get the last N photos
                photosSubset.addAll(photosList.subList((photosList.size() - cleanedDesiredCount), photosList.size()));
            }
            else {
                // get the first N photos
                photosSubset.addAll(photosList.subList(0, cleanedDesiredCount));
            }
            return photosSubset;
        }

        return photos;
    }

    /**
     * Returns a {@link Map} of photo channels (a {@link String} which is of the form {connector_pretty_name}.{object_name})
     * mapped to a {@link TimeInterval} which specifies the time range for that channel.  May return an empty
     * {@link Map}, but guaranteed to not return <code>null</code>.
     */
    @Override
    public Map<String, TimeInterval> getPhotoChannelTimeRanges(long guestId, final CoachingBuddy coachee) {
        // TODO: This could really benefit from some caching.  The time ranges can only change upon updating a photo
        // connector so it would be better to cache this info and then just refresh it whenever the connector is updated

        Map<String, TimeInterval> photoChannelTimeRanges = new HashMap<String, TimeInterval>();

        List<ApiKey> userKeys = guestService.getApiKeys(guestId);
        for (ApiKey key : userKeys) {
            Connector connector = null;
            if(key!=null) {
                connector = key.getConnector();
            }
            if (connector!=null && connector.getName()!=null &&
                (coachee==null||coachee.hasAccessToConnector(connector.getName()))
                && connector.hasImageObjectType()) {
                // Check the object types, if any, to find the image object type(s)
                ObjectType[] objectTypes = key.getConnector().objectTypes();
                if (objectTypes == null) {
                    final String channelName = constructChannelName(connector, null);
                    final TimeInterval timeInterval = constructTimeIntervalFromOldestAndNewestFacets(guestId, connector, null);
                    photoChannelTimeRanges.put(channelName, timeInterval);
                }
                else {
                    for (ObjectType objectType : objectTypes) {
                        if (objectType.isImageType()) {
                            final String channelName = constructChannelName(connector, objectType);
                            final TimeInterval timeInterval = constructTimeIntervalFromOldestAndNewestFacets(guestId, connector, null);
                            photoChannelTimeRanges.put(channelName, timeInterval);
                        }
                    }
                }
            }
        }
        return photoChannelTimeRanges;
    }

    /**
     * Returns all photos for {@link Connector}(s) specified by the given <code>connectorPrettyName</code>, and
     * optionally narrowed by the {@link ObjectType} specified by the given <code>objectTypeName</code>.  If the
     * <code>connectorPrettyName</code> is equal to the {@link #ALL_DEVICES_NAME}, then this method checks every
     * Connector for whether it has an image ObjectType and, if so, adds the relevant photos from each image ObjectType
     * belonging to the Connector.  If the <code>connectorPrettyName</code> is not equal to the
     * {@link #ALL_DEVICES_NAME}, then this method finds the specified Connector and ObjectType and adds the photos. May
     * return an empty {@link SortedSet}, but guaranteeed to not return <code>null</code>.
     */
    private SortedSet<Photo> getPhotos(final long guestId,
                                       final TimeInterval timeInterval,
                                       final String connectorPrettyName,
                                       final String objectTypeName,
                                       final PhotoFacetFinderStrategy facetFinderStrategy)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        SortedSet<Photo> photos = new TreeSet<Photo>();

        if (ALL_DEVICES_NAME.equals(connectorPrettyName)) {
            List<ApiKey> userKeys = guestService.getApiKeys(guestId);
            for (ApiKey key : userKeys) {
                Connector connector = null;
                if(key!=null && key.getConnector()!=null) {
                    connector = key.getConnector();
                }
                if (connector!=null && connector.hasImageObjectType()) {
                    final ObjectType[] objectTypes = connector.objectTypes();
                    if (objectTypes != null) {
                        for (ObjectType objectType : objectTypes) {
                            if (objectType.isImageType()) {
                                List<AbstractFacet> facets = facetFinderStrategy.find(connector, objectType);
                                photos.addAll(convertFacetsToPhotos(guestId, timeInterval, facets, connector, objectType));
                            }
                        }
                    }
                }
            }
        }
        else {
            final Connector connector = findConnectorByPrettyName(guestId, connectorPrettyName);
            if (connector != null) {
                final ObjectType desiredObjectType = findObjectTypeByName(connector, objectTypeName);
                if (desiredObjectType != null && desiredObjectType.isImageType()) {
                    List<AbstractFacet> facets = facetFinderStrategy.find(connector, desiredObjectType);
                    photos.addAll(convertFacetsToPhotos(guestId, timeInterval, facets, connector, desiredObjectType));
                }
            }
        }

        return photos;
    }

    /** Returns the Connector having the given pretty name.  Returns <code>null</code> if no such connector exists. */
    private Connector findConnectorByPrettyName(final long guestId, final String connectorPrettyName) {
        List<ApiKey> userKeys = guestService.getApiKeys(guestId);
        for (ApiKey key : userKeys) {
            if(key!=null) {
                final Connector connector = key.getConnector();
                if (connector!=null && connector.prettyName()!=null && connector.prettyName().equals(connectorPrettyName)) {
                    return connector;
                }
            }
        }

        return null;
    }

    /**
     * Returns the ObjectType for the given Connector having the given name.  If no such ObjectType exists, or
     * the connector doesn't have any ObjectTypes, or then this method returns <code>null</code>.
     */
    private ObjectType findObjectTypeByName(final Connector connector, final String objectTypeName) {
        if (connector != null && objectTypeName != null) {
            ObjectType[] objectTypes = connector.objectTypes();
            if (objectTypes != null) {
                for (final ObjectType objectType : objectTypes) {
                    if (objectTypeName.equals(objectType.getName())) {
                        return objectType;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Converts {@link AbstractFacet}s to {@link Photo}s.  If the given {@link TimeInterval} is <code>null</code>, this
     * method creates a new one for each {@link AbstractFacet} using the facet's start time.
     */
    private SortedSet<Photo> convertFacetsToPhotos(final long guestId,
                                                   final TimeInterval timeInterval,
                                                   final List<AbstractFacet> facets,
                                                   final Connector connector,
                                                   final ObjectType objectType)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        SortedSet<Photo> photos = new TreeSet<Photo>();

        GuestSettings settings = settingsService.getSettings(guestId);
        for (AbstractFacet facet : facets) {
            Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO.getFacetVOClass(facet);
            AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>)jsonFacetClass.newInstance();

            final TimeInterval actualTimeInterval;
            if (timeInterval == null) {
                // TODO: Not sure if this is correct for time zones...
                actualTimeInterval = new TimeInterval(facet.start, facet.start, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));
            }
            else {
                actualTimeInterval = timeInterval;
            }

            facetVo.extractValues(facet, actualTimeInterval, settings);
            photos.add(new PhotoImpl((AbstractPhotoFacetVO)facetVo, connector, objectType));
        }

        return photos;
    }

    /**
     * Constructs a channel name as the concatenation of the Connector pretty name and the ObjectType name.  Uses
     * the {@link #DEFAULT_PHOTOS_CHANNEL_NAME} if the ObjectType is <code>null</code>.
     */
    private String constructChannelName(final Connector connector, final ObjectType objectType) {
        return connector.prettyName() + "." + (objectType == null ? DEFAULT_PHOTOS_CHANNEL_NAME : objectType.getName());
    }

    private TimeInterval constructTimeIntervalFromOldestAndNewestFacets(final long guestId, final Connector connector, final ObjectType objectType) {
        final AbstractFacet oldestFacet = apiDataService.getOldestApiDataFacet(guestId, connector, objectType);
        final AbstractFacet newestFacet = apiDataService.getLatestApiDataFacet(guestId, connector, objectType);

        // TODO: Not sure if this is correct for time zones...
        return new TimeInterval(oldestFacet.start, newestFacet.start, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));
    }

    private static final class PhotoImpl implements Photo, Comparable<Photo> {

        private final AbstractPhotoFacetVO facetVo;
        private final Connector connector;
        private final ObjectType objectType;

        private PhotoImpl(final AbstractPhotoFacetVO facetVo, final Connector connector, final ObjectType objectType) {
            this.facetVo = facetVo;
            this.connector = connector;
            this.objectType = objectType;
        }

        @Override
        public AbstractPhotoFacetVO getAbstractPhotoFacetVO() {
            return facetVo;
        }

        @Override
        public Connector getConnector() {
            return connector;
        }

        @Override
        public ObjectType getObjectType() {
            return objectType;
        }

        @Override
        public int compareTo(final Photo that) {

            final Long thisStart = this.getAbstractPhotoFacetVO().start;
            final Long thatStart = that.getAbstractPhotoFacetVO().start;
            int comparison = thisStart.compareTo(thatStart);
            if (comparison != 0) {
                return comparison;
            }

            final String thisId = this.getConnector().getName() + "." + this.getObjectType().getName() + "." + this.getAbstractPhotoFacetVO().id + "." + this.getAbstractPhotoFacetVO().getPhotoUrl();
            final String thatId = that.getConnector().getName() + "." + that.getObjectType().getName() + "." + that.getAbstractPhotoFacetVO().id + "." + that.getAbstractPhotoFacetVO().getPhotoUrl();

            return thisId.compareTo(thatId);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final PhotoImpl photo = (PhotoImpl)o;

            if (connector != null ? !connector.equals(photo.connector) : photo.connector != null) {
                return false;
            }
            if (facetVo != null ? !facetVo.equals(photo.facetVo) : photo.facetVo != null) {
                return false;
            }
            if (objectType != null ? !objectType.equals(photo.objectType) : photo.objectType != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = facetVo != null ? facetVo.hashCode() : 0;
            result = 31 * result + (connector != null ? connector.hashCode() : 0);
            result = 31 * result + (objectType != null ? objectType.hashCode() : 0);
            return result;
        }
    }
}
