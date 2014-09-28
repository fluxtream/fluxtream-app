package org.fluxtream.connectors.lastfm;

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
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.springframework.stereotype.Component;

/**
 * <p>
 * <code>LastFmBodytrackResponder</code> does something...
 * </p>
 *
 * @author Anne Wright (anne.r.wright@gmail.com)
 */
@Component
public class LastFmBodytrackResponder extends AbstractBodytrackResponder {
    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        Connector connector = apiKey.getConnector();
        final ObjectType recent_track = ObjectType.getObjectType(connector, "recent_track");

        String objectTypeName = apiKey.getConnector().getName() + "-" + recent_track.getName();
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey, recent_track);

        // Sadly, the start and end times of track facets are the same.  Assume that the
        // start time is correct and arbitrarily draw a box that's 3 mins or
        // 1/256 of the tile width, whichever is larger.
        long duration = Math.max((endMillis-startMillis)/256L, 180000L);

        for (AbstractFacet facet : facets){
            LastFmRecentTrackFacet trackFacet = (LastFmRecentTrackFacet) facet;

            items.add(new TimespanModel(trackFacet.start,trackFacet.start+duration,"on",objectTypeName));
        }

        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings, final ApiKey apiKey, final String objectTypeName, final long start, final long end, final String value) {
        Connector connector = apiKey.getConnector();

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        final ObjectType recent_track = ObjectType.getObjectType(connector, "recent_track");

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, recent_track);

        return getFacetVOsForFacets(facets,timeInterval,guestSettings);
    }
}
