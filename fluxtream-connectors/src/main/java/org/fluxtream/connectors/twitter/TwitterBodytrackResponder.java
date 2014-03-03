package org.fluxtream.connectors.twitter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.SimpleTimeInterval;
import org.fluxtream.TimeInterval;
import org.fluxtream.TimeUnit;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.connectors.vos.AbstractFacetVO;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.TimespanModel;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 28/10/13
 * Time: 13:27
 */
@Component
public class TwitterBodytrackResponder extends AbstractBodytrackResponder {

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        Connector connector = apiKey.getConnector();

        // Sadly, the start and end times of twitter facets are the same.  Assume that the
        // start time is correct and arbitrarily draw a box that's 1 mins or
        // 1/256 of the tile width, whichever is larger.
        long duration = Math.max((endMillis-startMillis)/256L, 60000L);

        for (ObjectType objectType : connector.objectTypes()) {
            String objectTypeName = apiKey.getConnector().getName() + "-" + objectType.getName();
            List<AbstractFacet> objectTypeFacets = getFacetsInTimespan(timeInterval,apiKey, objectType);

            for (AbstractFacet facet : objectTypeFacets){
                items.add(new TimespanModel(facet.start, facet.start+duration, channelName, objectTypeName));
            }
        }

        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings, final ApiKey apiKey, final String objectTypeName, final long start, final long end, final String value) {
        Connector connector = apiKey.getConnector();

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        final ObjectType objectType = ObjectType.getObjectType(connector, value);

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, objectType);

        return getFacetVOsForFacets(facets,timeInterval,guestSettings);
    }

}
