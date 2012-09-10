package com.fluxtream.services.impl;

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

    @Override
    public SortedSet<Photo> getPhotos(long guestId, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<ApiKey> userKeys = guestService.getApiKeys(guestId);
        SortedSet<Photo> photos = new TreeSet<Photo>();
        for (ApiKey key : userKeys) {
            final Connector connector = key.getConnector();
            if (!connector.hasImageObjectType()) {
                continue;
            }
            ObjectType[] objectTypes = connector.objectTypes();
            if (objectTypes == null) {
                List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId, connector, null, timeInterval);
                photos.addAll(convertFacetsToVOs(guestId, timeInterval, facets, connector, null));
            }
            else {
                for (ObjectType objectType : objectTypes) {
                    if (objectType.isImageType()) {
                        List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId, connector, objectType, timeInterval);
                        photos.addAll(convertFacetsToVOs(guestId, timeInterval, facets, connector, objectType));
                    }
                }
            }
        }
        return photos;
    }

    @Override
    public SortedSet<Photo> getPhotos(final long guestId, final TimeInterval timeInterval, final String connectorPrettyName, final String objectTypeName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        if ("All".equals(connectorPrettyName) && "photos".equals(objectTypeName)) {
            return getPhotos(guestId, timeInterval);
        }

        SortedSet<Photo> photos = new TreeSet<Photo>();
        final Connector connector = findConnectorByPrettyName(guestId, connectorPrettyName);
        if (connector != null && connector.hasImageObjectType()) {

            ObjectType[] objectTypes = connector.objectTypes();
            if (objectTypes == null) {
                List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId, connector, null, timeInterval);
                photos.addAll(convertFacetsToVOs(guestId, timeInterval, facets, connector, null));
            }
            else {
                for (ObjectType objectType : objectTypes) {
                    if (objectType.isImageType() && objectType.getName().equals(objectTypeName)) {
                        List<AbstractFacet> facets = apiDataService.getApiDataFacets(guestId, connector, objectType, timeInterval);
                        photos.addAll(convertFacetsToVOs(guestId, timeInterval, facets, connector, objectType));
                        break;
                    }
                }
            }
        }

        return photos;
    }

    private Connector findConnectorByPrettyName(final long guestId, final String connectorPrettyName) {
        List<ApiKey> userKeys = guestService.getApiKeys(guestId);
        for (ApiKey key : userKeys) {
            final Connector connector = key.getConnector();
            if (connector.prettyName().equals(connectorPrettyName)) {
                return connector;
            }
        }

        return null;
    }

    private SortedSet<Photo> convertFacetsToVOs(final long guestId, final TimeInterval timeInterval, final List<AbstractFacet> facets, final Connector connector, final ObjectType objectType) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        GuestSettings settings = settingsService.getSettings(guestId);
        SortedSet<Photo> photos = new TreeSet<Photo>();
        for (AbstractFacet facet : facets) {
            Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO.getFacetVOClass(facet);
            AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>)jsonFacetClass.newInstance();
            facetVo.extractValues(facet, timeInterval, settings);
            photos.add(new PhotoImpl((AbstractPhotoFacetVO)facetVo, connector, objectType));
        }
        return photos;
    }

    /**
     * Returns a {@link Map} of photo channels (a {@link String} which is of the form {connector_pretty_name}.{object_name})
     * mapped to a {@link TimeInterval} which specifies the time range for that channel.  May return an empty
     * {@link Map}, but guaranteed to not return <code>null</code>.
     */
    @Override
    public Map<String, TimeInterval> getPhotoChannelTimeRanges(long guestId) {
        // TODO: This could really benefit from some caching.  The time ranges can only change upon updating a photo
        // connector so it would be better to cache this info and then just refresh it whenever the connector is updated

        Map<String, TimeInterval> photoChannelTimeRanges = new HashMap<String, TimeInterval>();

        List<ApiKey> userKeys = guestService.getApiKeys(guestId);
        for (ApiKey key : userKeys) {
            final Connector connector = key.getConnector();
            if (connector.hasImageObjectType()) {
                // Check the object types, if any, to find the image object type(s)
                ObjectType[] objectTypes = key.getConnector().objectTypes();
                if (objectTypes == null) {
                    final String channelName = constructChannelName(connector, "photos");
                    final TimeInterval timeInterval = constructTimeIntervalFromOldestAndNewestFacets(guestId, connector, null);
                    photoChannelTimeRanges.put(channelName, timeInterval);
                }
                else {
                    for (ObjectType objectType : objectTypes) {
                        if (objectType.isImageType()) {
                            final String objectName = objectType.name();
                            final String channelName = constructChannelName(connector, objectName);
                            final TimeInterval timeInterval = constructTimeIntervalFromOldestAndNewestFacets(guestId, connector, null);
                            photoChannelTimeRanges.put(channelName, timeInterval);
                        }
                    }
                }
            }
        }
        return photoChannelTimeRanges;
    }

    private String constructChannelName(final Connector connector, final String objectName) {
        return connector.prettyName() + "." + objectName;
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
            int comparison = (int)(this.getAbstractPhotoFacetVO().start - that.getAbstractPhotoFacetVO().start);
            if (comparison != 0) {
                return comparison;
            }
            comparison = connector.getName().compareTo(that.getConnector().getName());
            if (comparison != 0) {
                return comparison;
            }
            if (objectType == null) {
                if (that.getObjectType() != null) {
                    return 1;
                }
            }
            else {
                if (that.getObjectType() == null) {
                    return -1;
                }
                else {
                    comparison = objectType.getName().compareTo(that.getObjectType().getName());
                    if (comparison != 0) {
                        return comparison;
                    }
                }
            }

            return this.getAbstractPhotoFacetVO().photoUrl.compareTo(that.getAbstractPhotoFacetVO().photoUrl);
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
