package org.fluxtream.connectors.twitter;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
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

        //TODO: support merging
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

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping twitterChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "tweet").value() + ObjectType.getObjectType(apiKey.getConnector(), "dm").value() +
                        ObjectType.getObjectType(apiKey.getConnector(), "mention").value(),
                apiKey.getConnector().getDeviceNickname(), "activity",
                apiKey.getConnector().getDeviceNickname(), "activity");
        channelMappings.add(twitterChannelMapping);
    }

}
