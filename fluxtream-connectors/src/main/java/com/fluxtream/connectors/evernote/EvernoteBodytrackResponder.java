package com.fluxtream.connectors.evernote;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.SimpleTimeInterval;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.bodytrackResponders.AbstractBodytrackResponder;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.TimespanModel;
import com.fluxtream.services.CoachingService;
import com.fluxtream.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 08/01/14
 * Time: 12:09
 */
@Component
public class EvernoteBodytrackResponder extends AbstractBodytrackResponder {

    @Autowired
    SettingsService settingsService;

    @Autowired
    CoachingService coachingService;

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis,
                                            final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        EvernoteConnectorSettings connectorSettings = (EvernoteConnectorSettings)settingsService.getConnectorSettings(apiKey.getId());
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        String objectTypeName = "Evernote-note";
        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval, apiKey, ObjectType.getObjectType(Connector.getConnector("evernote"), "note"));
        facets = coachingService.filterFacets(AuthHelper.getGuestId(), apiKey.getId(), facets);

        // The start and end times of track facets are the same.  Assume that the
        // start time is correct and arbitrarily draw a box that's 7 mins or
        // 1/100th of the tile width, whichever is larger.
        long duration = Math.max((endMillis-startMillis)/100L, 60000*7L);


        for (AbstractFacet facet : facets){
            EvernoteNoteFacet note = (EvernoteNoteFacet) facet;
            if (connectorSettings!=null) {
                final NotebookConfig notebookConfig = connectorSettings.getNotebook(note.notebookGuid);
                if (notebookConfig!=null&&!notebookConfig.hidden)
                    items.add(new TimespanModel(note.start, note.start + duration, ((EvernoteNoteFacet)facet).notebookGuid, objectTypeName));
            } else
                items.add(new TimespanModel(note.start, note.start + duration, ((EvernoteNoteFacet)facet).notebookGuid, objectTypeName));
        }
        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings, final ApiKey apiKey,
                                                            final String objectTypeName, final long start, final long end,
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
            EvernoteNoteFacet event = (EvernoteNoteFacet) facet;
            if (event.notebookGuid.equals(value))
                filteredFacets.add(event);
        }

        List<AbstractFacetVO<AbstractFacet>> facetVOsForFacets = getFacetVOsForFacets(filteredFacets, timeInterval, guestSettings);
        return facetVOsForFacets;
    }
}
