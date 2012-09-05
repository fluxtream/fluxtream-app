package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.connectors.vos.AbstractInstantFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
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
    public boolean hasPhotos(final Guest guest) {
        if (guest != null) {
            List<ApiKey> userKeys = guestService.getApiKeys(guest.getId());
            for (ApiKey key : userKeys) {
                if (key.getConnector().hasImageObjectType()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<AbstractInstantFacetVO<AbstractFacet>> getPhotos(Guest guest, TimeInterval timeInterval) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        GuestSettings settings = settingsService.getSettings(guest.getId());
        List<ApiKey> userKeys = guestService.getApiKeys(guest.getId());
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        for (ApiKey key : userKeys) {
            if (!key.getConnector().hasImageObjectType()) {
                continue;
            }
            ObjectType[] objectTypes = key.getConnector().objectTypes();
            if (objectTypes == null) {
                facets.addAll(apiDataService.getApiDataFacets(guest.getId(), key.getConnector(), null, timeInterval));
            }
            else {
                for (ObjectType objectType : objectTypes) {
                    if (objectType.isImageType()) {
                        facets.addAll(apiDataService.getApiDataFacets(guest.getId(), key.getConnector(), objectType, timeInterval));
                    }
                }
            }
        }
        List<AbstractInstantFacetVO<AbstractFacet>> photos = new ArrayList<AbstractInstantFacetVO<AbstractFacet>>();
        for (AbstractFacet facet : facets) {
            Class<? extends AbstractFacetVO<AbstractFacet>> jsonFacetClass = AbstractFacetVO.getFacetVOClass(facet);
            AbstractInstantFacetVO<AbstractFacet> facetVo = (AbstractInstantFacetVO<AbstractFacet>)jsonFacetClass.newInstance();
            facetVo.extractValues(facet, timeInterval, settings);
            photos.add(facetVo);
        }
        return photos;
    }

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
}
