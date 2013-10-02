package com.fluxtream.connectors.bodytrackResponders;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.vos.AbstractFacetVO;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ChannelMapping;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.TimespanModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractBodytrackResponder {

    @Autowired
    protected MetadataService metadataService;

    @Autowired
    protected ApiDataService apiDataService;

    @Autowired
    protected GuestService guestService;

    public static class Bounds{
        public double min;
        public double max;
        public double min_time;
        public double max_time;
    }

    protected static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    protected List<AbstractFacet> getFacetsInTimespan(TimeInterval timeInterval, ApiKey apiKey, ObjectType objectType){
        /*if (objectType.isDateBased()){          //TODO: determine whether or not date based queries are necessary
            List<String> dates = new ArrayList<String>();
            DateTime start = new DateTime(timeInterval.getStart());
            DateTime end = new DateTime(timeInterval.getEnd());
            while (start.isBefore(end)){
                dates.add(dateFormatter.print(start));
                start = start.plusDays(1);
            }
            String endDate = dateFormatter.print(end);
            if (!dates.contains(endDate)) dates.add(endDate);

            return apiDataService.getApiDataFacets(apiKey,objectType,dates);

        }
        else{  */
            return apiDataService.getApiDataFacets(apiKey,objectType,timeInterval);
        //}
    }

    protected List<AbstractFacetVO<AbstractFacet>> getFacetVOsForFacets(List<AbstractFacet> facets,TimeInterval timeInterval, GuestSettings guestSettings){
        List<AbstractFacetVO<AbstractFacet>> facetVOs = new ArrayList<AbstractFacetVO<AbstractFacet>>();

        for (AbstractFacet facet : facets){
            try{
                AbstractFacetVO<AbstractFacet> facetVO = AbstractFacetVO.getFacetVOClass(facet).newInstance();
                facetVO.extractValues(facet,timeInterval,guestSettings);
                facetVOs.add(facetVO);
            }
            catch (Exception e){
                e.printStackTrace();

            }
        }
        return facetVOs;
    }


    public abstract List<TimespanModel> getTimespans(long startMillis, long endMillis, ApiKey apiKey, String channelName);

    public abstract List<AbstractFacetVO<AbstractFacet>> getFacetVOs(GuestSettings guestSettings, ApiKey apiKey, String objectTypeName,long start,long end,String value);

    public Bounds getBounds(final ChannelMapping mapping) {
        Bounds bounds = new Bounds();
        switch (mapping.channelType){
            case photo:
                bounds.min = 0.6;
                bounds.max = 1;
                break;
            default:
                bounds.min = 0;
                bounds.max = 1;
                break;
        }
        ApiKey apiKey = guestService.getApiKey(mapping.apiKeyId);
        if (mapping.objectTypeId == null){
            bounds.min_time = Double.MAX_VALUE;
            bounds.max_time = Double.MIN_VALUE;
            for (ObjectType objectType : apiKey.getConnector().objectTypes()){
                AbstractFacet facet = apiDataService.getOldestApiDataFacet(apiKey,objectType);
                bounds.min_time = Math.min(bounds.min_time,facet.start / 1000.0);
                facet = apiDataService.getLatestApiDataFacet(apiKey,objectType);
                bounds.max_time = Math.max(bounds.max_time,facet.end / 1000.0);

            }
            if (bounds.max_time < bounds.min_time){
                bounds.min_time = bounds.max_time = 0;
            }
        }
        else{
            AbstractFacet facet = apiDataService.getOldestApiDataFacet(apiKey,ObjectType.getObjectType(apiKey.getConnector(),mapping.objectTypeId));
            bounds.min_time = facet.start / 1000.0;
            facet = apiDataService.getLatestApiDataFacet(apiKey,ObjectType.getObjectType(apiKey.getConnector(),mapping.objectTypeId));
            bounds.max_time = facet.end / 1000.0;
        }
        return bounds;
    }
}
