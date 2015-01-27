package org.fluxtream.connectors.fitbit;

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
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 28/10/13
 * Time: 10:13
 */
@Component
public class FitbitBodytrackResponder extends AbstractBodytrackResponder {

    @Autowired
    MetadataService metadataService;

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        Connector connector = apiKey.getConnector();
        final ObjectType sleep = ObjectType.getObjectType(connector, "sleep");

        String objectTypeName = apiKey.getConnector().getName() + "-" + sleep.getName();
        List<AbstractFacet> facets = getFacetsInTimespanOrderedByEnd(timeInterval,apiKey, sleep);

        for (AbstractFacet facet : facets){
            FitbitSleepFacet sleepFacet = (FitbitSleepFacet)facet;
            TimeZone timeZone = metadataService.getTimeZone(apiKey.getGuestId(), sleepFacet.start);
            long userStart = sleepFacet.start - timeZone.getOffset(sleepFacet.start);
            long userEnd = sleepFacet.end - timeZone.getRawOffset();
            simpleMergeAddTimespan(items, new TimespanModel(userStart, userEnd, "on",objectTypeName),startMillis,endMillis);
        }

        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings, final ApiKey apiKey,
                                                            final String objectTypeName,
                                                            final long start, final long end,
                                                            final String value) {
        Connector connector = apiKey.getConnector();

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        final ObjectType sleep = ObjectType.getObjectType(connector, "sleep");

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, sleep);

        return getFacetVOsForFacets(facets, timeInterval, guestSettings);
    }

    @Override
    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping sleepChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "sleep").value(),
                apiKey.getConnector().getDeviceNickname(), "sleep",
                apiKey.getConnector().getDeviceNickname(), "sleep");
        channelMappings.add(sleepChannelMapping);
    }

}
