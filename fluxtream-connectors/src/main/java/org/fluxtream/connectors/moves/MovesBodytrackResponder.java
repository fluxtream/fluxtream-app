package org.fluxtream.connectors.moves;

import org.fluxtream.core.Configuration;
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
import org.fluxtream.core.domain.metadata.FoursquareVenue;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

@Component
public class MovesBodytrackResponder extends AbstractBodytrackResponder {

    @Autowired
    Configuration env;

    @Autowired
    JPADaoService jpaDaoService;

    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));
        ObjectType[] objectTypes = apiKey.getConnector().objectTypes();

        //TODO: support merging
        for (ObjectType objectType : objectTypes){
            String objectTypeName = apiKey.getConnector().getName() + "-" + objectType.getName();
            if (objectType.getName().equals("move")){
                List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,objectType);
                for (AbstractFacet facet : facets){
                    MovesMoveFacet moveFacet = (MovesMoveFacet) facet;
                    for (MovesActivity activity : moveFacet.getActivities()){
                        BodyTrackHelper.TimespanStyle style = new BodyTrackHelper.TimespanStyle();
                        style.iconURL = String.format(env.get("homeBaseUrl")+"%s/images/moves/" + activity.activity + ".png", env.get("release"));
                        final TimespanModel moveTimespanModel = new TimespanModel(activity.start, activity.end, activity.activity, objectTypeName, style);
                        items.add(moveTimespanModel);
                    }
                }

            }
            else if (objectType.getName().equals("place")){
                List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,objectType);
                for (AbstractFacet facet : facets){
                    MovesPlaceFacet place = (MovesPlaceFacet) facet;
                    BodyTrackHelper.TimespanStyle style = new BodyTrackHelper.TimespanStyle();
                    style.iconURL = getMovesPlaceIcon(place.apiKeyId, place.getId());
                    final TimespanModel placeTimespanModel = new TimespanModel(place.start, place.end, "place", objectTypeName,style);
                    items.add(placeTimespanModel);
                }
            }
        }
        return items;
    }


    public String getMovesPlaceIcon(@PathParam("apiKeyId") long apiKeyId,
                                    @PathParam("id") long id) {
        List l = jpaDaoService.executeNativeQuery("SELECT type, foursquareId FROM Facet_MovesPlace WHERE apiKeyId=(?1) AND id=(?2)", apiKeyId, id);
        String homeBaseUrl = env.get("homeBaseUrl");
        if (l==null||l.size()==0)
            return homeBaseUrl+"images/moves/unknown.png";
        final Object[] singleResult = (Object[])l.get(0);
        String type = (String) singleResult[0];
        if (type.equals("foursquare")) {
            String foursquareId = (String) singleResult[1];
            final FoursquareVenue foursquareVenue = metadataService.getFoursquareVenue(foursquareId);
            return foursquareVenue.categoryIconUrlPrefix + "bg_32" + foursquareVenue.categoryIconUrlSuffix;
        } else {
            return homeBaseUrl+"images/moves/" + type + ".png";
        }
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(final GuestSettings guestSettings, final ApiKey apiKey, final String objectTypeName, final long start, final long end, final String value) {
        Connector connector = apiKey.getConnector();
        String[] objectTypeNameParts = objectTypeName.split("-");
        ObjectType objectType = null;
        for (ObjectType ot : connector.objectTypes()){
            if (ot.getName().equals(objectTypeNameParts[1])){
                objectType = ot;
                break;
            }
        }
        if (objectType == null || (objectType.getName().equals("place") && !"place".equals(value)))
            return new ArrayList<AbstractFacetVO<AbstractFacet>>();

        TimeInterval timeInterval = metadataService.getArbitraryTimespanMetadata(apiKey.getGuestId(), start, end).getTimeInterval();

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,objectType);

        if (objectType.getName().equals("move")){
            MovesMoveFacet move;
            for (Iterator<AbstractFacet> i = facets.iterator(); i.hasNext();){
                move = (MovesMoveFacet) i.next();
                boolean found = false;
                for (MovesActivity activity : move.getActivities()){
                    if (activity.activity.equals(value)){
                        found = true;
                        break;
                    }
                }
                if (!found)
                   i.remove();
            }
        }

        List<AbstractFacetVO<AbstractFacet>> facetVOsForFacets = getFacetVOsForFacets(facets, timeInterval, guestSettings);
        facetVOsForFacets = dedup(facetVOsForFacets);
        return facetVOsForFacets;
    }

    private List<AbstractFacetVO<AbstractFacet>> dedup(final List<AbstractFacetVO<AbstractFacet>> facetVOs) {
        List<AbstractFacetVO<AbstractFacet>> deduped = new ArrayList<AbstractFacetVO<AbstractFacet>>();

        there: for (AbstractFacetVO<AbstractFacet> facetVO : facetVOs) {
            AbstractMovesFacetVO f = (AbstractMovesFacetVO) facetVO;
            for (AbstractFacetVO<AbstractFacet> uniqueFacet : deduped) {
                AbstractMovesFacetVO u = (AbstractMovesFacetVO) uniqueFacet;
                if (u.type.equals(f.type)&&u.start==f.start) {
                    if (u.end>f.end)
                        continue there;
                    else {
                        deduped.remove(u);
                        deduped.add(f);
                        continue there;
                    }
                }
            }
            deduped.add(f);
        }
        return deduped;
    }

    public void addToDeclaredChannelMappings(final ApiKey apiKey, final List<ChannelMapping> channelMappings) {
        ChannelMapping movesDataChannelMapping = new ChannelMapping(
                apiKey.getId(), apiKey.getGuestId(),
                ChannelMapping.ChannelType.timespan,
                ChannelMapping.TimeType.gmt,
                ObjectType.getObjectType(apiKey.getConnector(), "move").value() + ObjectType.getObjectType(apiKey.getConnector(), "place").value(),
                apiKey.getConnector().getDeviceNickname(), "data",
                apiKey.getConnector().getDeviceNickname(), "data");
        channelMappings.add(movesDataChannelMapping);
    }

}
