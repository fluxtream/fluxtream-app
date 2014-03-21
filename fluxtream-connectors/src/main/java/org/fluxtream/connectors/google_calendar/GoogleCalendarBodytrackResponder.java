package org.fluxtream.connectors.google_calendar;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.SimpleTimeInterval;
import org.fluxtream.TimeInterval;
import org.fluxtream.TimeUnit;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.connectors.vos.AbstractFacetVO;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.GuestSettings;
import org.fluxtream.mvc.models.TimespanModel;
import org.fluxtream.services.CoachingService;
import org.fluxtream.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 19/10/13
 * Time: 00:25
 */
@Component
public class GoogleCalendarBodytrackResponder extends AbstractBodytrackResponder {

    @Autowired
    SettingsService settingsService;

    @Autowired
    CoachingService coachingService;

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        GoogleCalendarConnectorSettings connectorSettings = (GoogleCalendarConnectorSettings)settingsService.getConnectorSettings(apiKey.getId());
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        String objectTypeName = apiKey.getConnector().getName() + "-event";
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,null);
        facets = coachingService.filterFacets(AuthHelper.getGuestId(), apiKey.getId(), facets);

        for (AbstractFacet facet : facets){
            GoogleCalendarEventFacet event = (GoogleCalendarEventFacet) facet;
            if (connectorSettings!=null) {
                final CalendarConfig calendarConfig = connectorSettings.getCalendar(event.calendarId);
                if (calendarConfig!=null&&!calendarConfig.hidden)
                    items.add(new TimespanModel(event.start, event.end, ((GoogleCalendarEventFacet)facet).calendarId, objectTypeName));
            } else
                items.add(new TimespanModel(event.start, event.end, ((GoogleCalendarEventFacet)facet).calendarId, objectTypeName));
        }
        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings,
                                                            final ApiKey apiKey,
                                                            final String objectTypeName,
                                                            final long start, final long end,
                                                            final String value) {
        Connector connector = apiKey.getConnector();
        String[] objectTypeNameParts = objectTypeName.split("-");
        ObjectType objectType = null;
        for (ObjectType ot : connector.objectTypes()){
            if (ot.getName().equals(objectTypeNameParts[1])){
                objectType = ot;
                break;
            }
        }

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,objectType);
        List<AbstractFacet> filteredFacets = new ArrayList<AbstractFacet>();
        for (AbstractFacet facet : facets) {
            GoogleCalendarEventFacet event = (GoogleCalendarEventFacet) facet;
            if (event.calendarId.equals(value))
                filteredFacets.add(event);
        }

        List<AbstractFacetVO<AbstractFacet>> facetVOsForFacets = getFacetVOsForFacets(filteredFacets, timeInterval, guestSettings);
        return facetVOsForFacets;
    }
}
