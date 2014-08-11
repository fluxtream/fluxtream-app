package org.fluxtream.core.connectors.bodytrackResponders;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.springframework.stereotype.Component;

@Component
public class DefaultBodytrackResponder extends AbstractBodytrackResponder {
    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, ApiKey apiKey, String channelName) {
        ObjectType objectType = null;
        ArrayList<TimespanModel> items = new ArrayList<TimespanModel>();
        for (ObjectType ot : apiKey.getConnector().objectTypes()){
            if (ot.getName().equals(channelName)){
                objectType = ot;
                break;
            }
        }
        if (objectType != null){
            String objectTypeName = apiKey.getConnector().getName() + "-" + objectType.getName();
            final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));

            List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,objectType);

            for (AbstractFacet facet : facets){
                items.add(new TimespanModel(facet.start,facet.end,"on",objectTypeName));
            }

        }
        return items;
    }

    @Override
    public List<AbstractFacetVO<AbstractFacet>> getFacetVOs(GuestSettings guestSettings, ApiKey apiKey, final String objectTypeName, final long start, final long end, final String value) {
        Connector connector = apiKey.getConnector();
        String[] objectTypeNameParts = objectTypeName.split("-");
        ObjectType objectType = null;
        for (ObjectType ot : connector.objectTypes()){
            if (ot.getName().equals(objectTypeNameParts[1])){
                objectType = ot;
                break;
            }
        }
        if (objectType == null)
            return new ArrayList<AbstractFacetVO<AbstractFacet>>();

        TimeInterval timeInterval = new SimpleTimeInterval(start, end, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));

        List<AbstractFacet> facets = getFacetsInTimespan(timeInterval,apiKey,objectType);

        return getFacetVOsForFacets(facets,timeInterval,guestSettings);
    }
}
