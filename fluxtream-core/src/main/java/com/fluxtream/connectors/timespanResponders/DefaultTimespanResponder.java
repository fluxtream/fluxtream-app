package com.fluxtream.connectors.timespanResponders;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.SimpleTimeInterval;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.mvc.models.TimespanModel;
import com.fluxtream.services.ApiDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultTimespanResponder extends AbstractTimespanResponder {
    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, ApiKey apiKey, String channelName, ApiDataService apiDataService) {
        ObjectType objectType = null;
        ArrayList<TimespanModel> items = new ArrayList<TimespanModel>();
        for (ObjectType ot : apiKey.getConnector().objectTypes()){
            if (ot.getName().equals(channelName)){
                objectType = ot;
                break;
            }
        }
        if (objectType != null){
            final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));

            List<AbstractFacet> facets = getFacetsInTimespan(apiDataService,timeInterval,apiKey,objectType);

            for (AbstractFacet facet : facets){
                items.add(new TimespanModel(facet.start,facet.end));
            }

        }
        return items;
    }
}
