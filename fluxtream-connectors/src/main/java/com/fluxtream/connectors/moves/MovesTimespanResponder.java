package com.fluxtream.connectors.moves;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.SimpleTimeInterval;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.timespanResponders.AbstractTimespanResponder;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.mvc.models.TimespanModel;
import com.fluxtream.services.ApiDataService;

public class MovesTimespanResponder extends AbstractTimespanResponder {
    @Override
    public List<TimespanModel> getTimespans(final long startMillis, final long endMillis, final ApiKey apiKey, final String channelName, final ApiDataService apiDataService) {
        List<TimespanModel> items = new ArrayList<TimespanModel>();
        final TimeInterval timeInterval = new SimpleTimeInterval(startMillis, endMillis, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));
        ObjectType[] objectTypes = apiKey.getConnector().objectTypes();

        for (ObjectType objectType : objectTypes){
            if (objectType.getName().equals("move")){
                List<AbstractFacet> facets = getFacetsInTimespan(apiDataService,timeInterval,apiKey,objectType);
                for (AbstractFacet facet : facets){
                    MovesMoveFacet moveFacet = (MovesMoveFacet) facet;
                    for (MovesActivity activity : moveFacet.getActivities()){
                        items.add(new TimespanModel(activity.start,activity.end,activity.activity));
                    }
                }

            }
            else if (objectType.getName().equals("place")){
                List<AbstractFacet> facets = getFacetsInTimespan(apiDataService,timeInterval,apiKey,objectType);
                for (AbstractFacet facet : facets){
                    MovesPlaceFacet place = (MovesPlaceFacet) facet;
                    items.add(new TimespanModel(place.start,place.end,"place"));
                }
            }
        }
        return items;
    }
}
