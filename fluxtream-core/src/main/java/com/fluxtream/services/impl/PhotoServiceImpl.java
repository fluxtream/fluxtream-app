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
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.domain.PhotoFacetFinderStrategy;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.PhotoService;
import com.fluxtream.services.SettingsService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
@Service
@Component
public class PhotoServiceImpl implements PhotoService {

    private static final Logger LOG = Logger.getLogger("Fluxtream");

    @Autowired
    SettingsService settingsService;

    @Autowired
    GuestService guestService;

    @Autowired
    BeanFactory beanFactory;

    private static abstract class PhotoFinder {
        final Map<ObjectType, List<AbstractFacet>> find(final ApiKey apiKey) {
            final Map<ObjectType, List<AbstractFacet>> facets = new HashMap<ObjectType, List<AbstractFacet>>();
            if (apiKey.getConnector()!= null) {
                final ObjectType[] objectTypes = apiKey.getConnector().objectTypes();
                if (objectTypes != null) {
                    for (final ObjectType objectType : objectTypes) {
                        if (objectType.isImageType()) {
                            facets.put(objectType, find(apiKey, objectType));
                        }
                    }
                }
            }
            return facets;
        }

        protected abstract List<AbstractFacet> find(final ApiKey apiKey, final ObjectType objectType);
    }

    @Override
    public SortedSet<Photo> getPhotos(ApiKey apiKey, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return getPhotos(apiKey, timeInterval, ALL_DEVICES_NAME, DEFAULT_PHOTOS_CHANNEL_NAME);
    }

    @Override
    public SortedSet<Photo> getPhotos(final ApiKey apiKey, final TimeInterval timeInterval, final String connectorPrettyName, final String objectTypeName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        return getPhotos(apiKey, timeInterval, connectorPrettyName, objectTypeName, new PhotoFinder() {
            public List<AbstractFacet> find(final ApiKey apiKey, final ObjectType objectType) {
                final PhotoFacetFinderStrategy photoFacetFinderStrategy = getPhotoFacetFinderStrategyFromObjectType(objectType);
                if (photoFacetFinderStrategy != null) {
                    return photoFacetFinderStrategy.findAll(apiKey, objectType, timeInterval);
                }
                return new ArrayList<AbstractFacet>(0);
            }
        });
    }

    @Override
    public SortedSet<Photo> getPhotos(final ApiKey apiKey, final long timeInMillis, final String connectorPrettyName, final String objectTypeName, final int desiredCount, final boolean isGetPhotosBeforeTime) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        // make sure the count is >= 1
        final int cleanedDesiredCount = Math.max(1, desiredCount);

        final SortedSet<Photo> photos = getPhotos(apiKey, null, connectorPrettyName, objectTypeName, new PhotoFinder() {

            public List<AbstractFacet> find(final ApiKey apiKey, final ObjectType objectType) {
                final PhotoFacetFinderStrategy photoFacetFinderStrategy = getPhotoFacetFinderStrategyFromObjectType(objectType);
                if (photoFacetFinderStrategy != null) {
                    if (isGetPhotosBeforeTime) {
                        return photoFacetFinderStrategy.findBefore(apiKey, objectType, timeInMillis, cleanedDesiredCount);
                    }
                    else {
                        return photoFacetFinderStrategy.findAfter(apiKey, objectType, timeInMillis, cleanedDesiredCount);
                    }
                }
                return new ArrayList<AbstractFacet>(0);
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

    @Override
    public Map<String, TimeInterval> getPhotoChannelTimeRanges(ApiKey apiKey, final CoachingBuddy coachee) {
        // TODO: This could really benefit from some caching.  The time ranges can only change upon updating a photo
        // connector so it would be better to cache this info and then just refresh it whenever the connector is updated

        Map<String, TimeInterval> photoChannelTimeRanges = new HashMap<String, TimeInterval>();

        List<ApiKey> userKeys = guestService.getApiKeys(apiKey.getGuestId());
        for (ApiKey key : userKeys) {
            Connector connector = null;
            if (key != null) {
                connector = key.getConnector();
            }
            if (connector != null && connector.getName() != null &&
                (coachee == null || coachee.hasAccessToConnector(connector.getName())) && connector.hasImageObjectType()) {
                // Check the object types, if any, to find the image object type(s)
                ObjectType[] objectTypes = key.getConnector().objectTypes();
                if (objectTypes == null) {
                    final String channelName = constructChannelName(connector, null);
                    final TimeInterval timeInterval = constructTimeIntervalFromOldestAndNewestFacets(apiKey, null);
                    photoChannelTimeRanges.put(channelName, timeInterval);
                }
                else {
                    for (ObjectType objectType : objectTypes) {
                        if (objectType.isImageType()) {
                            final String channelName = constructChannelName(connector, objectType);
                            final TimeInterval timeInterval = constructTimeIntervalFromOldestAndNewestFacets(apiKey, objectType);
                            photoChannelTimeRanges.put(channelName, timeInterval);
                        }
                    }
                }
            }
        }
        return photoChannelTimeRanges;
    }

    private PhotoFacetFinderStrategy getPhotoFacetFinderStrategyFromObjectType(final ObjectType objectType) {
        if (objectType != null) {
            try {
                final Class<? extends AbstractFacet> facetClass = objectType.facetClass();
                final ObjectTypeSpec objectTypeSpec = facetClass.getAnnotation(ObjectTypeSpec.class);
                final Class<? extends PhotoFacetFinderStrategy> photoFacetFinderStrategyClass = objectTypeSpec.photoFacetFinderStrategy();
                return beanFactory.getBean(photoFacetFinderStrategyClass);
            }
            catch (Exception e) {
                LOG.error("Exception caught while trying trying to instantiate the PhotoFacetFinderStrategy from objectType [" + objectType + "].  Returning null.", e);
            }
        }
        return null;
    }

    /**
     * Returns all photos for {@link Connector}(s) specified by the given <code>connectorPrettyName</code>, and
     * optionally narrowed by the {@link ObjectType} specified by the given <code>objectTypeName</code>.  If the
     * <code>connectorPrettyName</code> is equal to the {@link #ALL_DEVICES_NAME}, then this method checks every
     * Connector for whether it has an image ObjectType and, if so, adds the relevant photos from each image ObjectType
     * belonging to the Connector.  If the <code>connectorPrettyName</code> is not equal to the
     * {@link #ALL_DEVICES_NAME}, then this method finds the specified Connector and ObjectType and adds the photos.
     * Furthermore, if the objectTypeName does not specify an existing ObjectType for the Connector, then this method
     * returns photos from from all ObjectTypes which are of {@link ObjectType#isImageType() image type}.
     *
     * May return an empty {@link SortedSet}, but guaranteeed to not return <code>null</code>.
     */
    private SortedSet<Photo> getPhotos(final ApiKey apiKey, final TimeInterval timeInterval, final String connectorPrettyName, final String objectTypeName, final PhotoFinder facetFinderStrategy) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        SortedSet<Photo> photos = new TreeSet<Photo>();

        if (ALL_DEVICES_NAME.equals(connectorPrettyName)) {
            List<ApiKey> userKeys = guestService.getApiKeys(apiKey.getGuestId());
            for (ApiKey key : userKeys) {
                Connector connector = null;
                if (key != null && key.getConnector() != null) {
                    connector = key.getConnector();
                }
                if (connector != null && connector.hasImageObjectType()) {
                    final ObjectType[] objectTypes = connector.objectTypes();
                    if (objectTypes != null) {
                        for (ObjectType objectType : objectTypes) {
                            if (objectType.isImageType()) {
                                List<AbstractFacet> facets = facetFinderStrategy.find(apiKey, objectType);
                                photos.addAll(convertFacetsToPhotos(apiKey, timeInterval, facets, connector, objectType));
                            }
                        }
                    }
                }
            }
        }
        else {
            final Connector connector = findConnectorByPrettyName(apiKey, connectorPrettyName);
            if (connector != null) {
                final ObjectType desiredObjectType = findObjectTypeByName(connector, objectTypeName);

                if (desiredObjectType == null) {
                    final Map<ObjectType, List<AbstractFacet>> facetsByObjectType = facetFinderStrategy.find(apiKey);
                    if ((facetsByObjectType != null) && (!facetsByObjectType.isEmpty())) {
                        for (final ObjectType objectType : facetsByObjectType.keySet()) {
                            final List<AbstractFacet> facets = facetsByObjectType.get(objectType);
                            if (facets != null) {
                                photos.addAll(convertFacetsToPhotos(apiKey, timeInterval, facets, connector, objectType));
                            }
                        }
                    }
                }
                else if (desiredObjectType.isImageType()) {
                    final List<AbstractFacet> facets = facetFinderStrategy.find(apiKey, desiredObjectType);
                    if (facets != null) {
                        photos.addAll(convertFacetsToPhotos(apiKey, timeInterval, facets, connector, desiredObjectType));
                    }
                }
            }
        }

        return photos;
    }

    /** Returns the Connector having the given pretty name.  Returns <code>null</code> if no such connector exists. */
    private Connector findConnectorByPrettyName(final ApiKey apiKey, final String connectorPrettyName) {
        List<ApiKey> userKeys = guestService.getApiKeys(apiKey.getGuestId());
        for (ApiKey key : userKeys) {
            if (key != null) {
                final Connector connector = key.getConnector();
                if (connector != null && connector.prettyName() != null && connector.prettyName().equals(connectorPrettyName)) {
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
    private SortedSet<Photo> convertFacetsToPhotos(final ApiKey apiKey, final TimeInterval timeInterval, final List<AbstractFacet> facets, final Connector connector, final ObjectType objectType) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        SortedSet<Photo> photos = new TreeSet<Photo>();

        GuestSettings settings = settingsService.getSettings(apiKey.getGuestId());
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

    /**
     * Returns the {@link TimeInterval} for the oldest and newest facets.  Returns <code>null</code> if no facets exist.
     */
    private TimeInterval constructTimeIntervalFromOldestAndNewestFacets(final ApiKey apiKey, final ObjectType objectType) {
        final PhotoFacetFinderStrategy photoFacetFinderStrategy = getPhotoFacetFinderStrategyFromObjectType(objectType);
        final AbstractFacet oldestFacet = photoFacetFinderStrategy.findOldest(apiKey, objectType);
        final AbstractFacet newestFacet = photoFacetFinderStrategy.findLatest(apiKey, objectType);

        if (oldestFacet != null && newestFacet != null) {
            // TODO: Not sure if this is correct for time zones...
            return new TimeInterval(oldestFacet.start, newestFacet.start, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));
        }

        return null;
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
