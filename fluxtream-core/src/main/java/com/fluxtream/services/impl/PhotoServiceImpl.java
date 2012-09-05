package com.fluxtream.services.impl;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.TimeInterval;
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
    public List<AbstractInstantFacetVO<AbstractFacet>> getPhotos(Guest guest, TimeInterval timeInterval) throws ClassNotFoundException,
                                                                                                                IllegalAccessException,
                                                                                                                InstantiationException {
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
                    facets.addAll(apiDataService.getApiDataFacets(guest.getId(), key.getConnector(), objectType, timeInterval));
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
}
