package org.fluxtream.connectors.fitbit;

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
 * Time: 10:13
 */
@Component
public class FitbitBodytrackResponder extends AbstractBodytrackResponder {

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        Connector connector = apiKey.getConnector();
        final ObjectType sleep = ObjectType.getObjectType(connector, "sleep");

        String objectTypeName = apiKey.getConnector().getName() + "-" + sleep.getName();
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey, sleep);

        for (AbstractFacet facet : facets){
            FitbitSleepFacet sleepFacet = (FitbitSleepFacet)facet;

            items.add(new TimespanModel(sleepFacet.start,sleepFacet.end, "on",objectTypeName));
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

}
