package org.fluxtream.connectors.lastfm;

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
