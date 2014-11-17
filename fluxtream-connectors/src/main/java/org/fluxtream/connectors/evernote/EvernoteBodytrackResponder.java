package org.fluxtream.connectors.evernote;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.fluxtream.core.services.BuddiesService;
import org.fluxtream.core.services.SettingsService;
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
    BuddiesService buddiesService;

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis,
                                            final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        EvernoteConnectorSettings connectorSettings = (EvernoteConnectorSettings)settingsService.getConnectorSettings(apiKey.getId());
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        String objectTypeName = "Evernote-note";
        List<AbstractFacet> facets = getFacetsInTimespanOrderedByEnd(timeInterval, apiKey, ObjectType.getObjectType(Connector.getConnector("evernote"), "note"));
        facets = buddiesService.filterFacets(AuthHelper.getGuestId(), apiKey.getId(), facets);

        // The start and end times of track facets are the same.  Assume that the
        // start time is correct and arbitrarily draw a box that's 7 mins or
        // 1/100th of the tile width, whichever is larger.
        long duration = Math.max((endMillis-startMillis)/100L, 60000*7L);


        for (AbstractFacet facet : facets){
            EvernoteNoteFacet note = (EvernoteNoteFacet) facet;
            if (connectorSettings!=null) {
                final NotebookConfig notebookConfig = connectorSettings.getNotebook(note.notebookGuid);
                if (notebookConfig!=null&&!notebookConfig.hidden)
                    simpleMergeAddTimespan(items,new TimespanModel(note.start, note.start + duration, ((EvernoteNoteFacet)facet).notebookGuid, objectTypeName),startMillis,endMillis);
            } else
                simpleMergeAddTimespan(items,new TimespanModel(note.start, note.start + duration, ((EvernoteNoteFacet)facet).notebookGuid, objectTypeName),startMillis,endMillis);
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
