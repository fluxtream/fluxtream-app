package org.fluxtream.core.connectors.bodytrackResponders;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiKeyAttribute;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.domain.GuestSettings;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
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

    protected List<AbstractFacet> getFacetsInTimespan(TimeInterval timeInterval, ApiKey apiKey, ObjectType objectType){
        //TODO: determine whether or not date based queries are necessary
        //if (objectType!=null&&objectType.isDateBased()){
        //    List<String> dates = new ArrayList<String>();
        //    org.joda.time.DateTime start = new org.joda.time.DateTime(timeInterval.getStart());
        //    org.joda.time.DateTime end = new org.joda.time.DateTime(timeInterval.getEnd());
        //    while (start.isBefore(end)){
        //        dates.add(dateFormatter.print(start));
        //        start = start.plusDays(1);
        //    }
        //    String endDate = dateFormatter.print(end);
        //    if (!dates.contains(endDate)) dates.add(endDate);
        //
        //    return apiDataService.getApiDataFacets(apiKey,objectType,dates);
        //
        //}
        //else {
            return apiDataService.getApiDataFacets(apiKey,objectType,timeInterval, null);
        //}
    }

    protected List<AbstractFacet> getFacetsInTimespanOrderedByEnd(TimeInterval timeInterval, ApiKey apiKey, ObjectType objectType){
        return apiDataService.getApiDataFacets(apiKey,objectType,timeInterval,null,"facet.end ASC", null);
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

    //note: assumption made: list will be sorted such that the last element in it will have the highest end time and newModel is the closest new model to that one
    public void simpleMergeAddTimespan(List<TimespanModel> list, TimespanModel newModel, long startMillis, long endMillis){
        final int largeWidth = 1920;
        final double millisPerPixel = (endMillis - startMillis) / (double) largeWidth;
        if (!list.isEmpty()){
            TimespanModel lastModel = list.get(list.size() - 1);
            if (lastModel.getObjectType().equals(newModel.getObjectType()) && lastModel.getValue().equals(newModel.getValue())){
                double millisFromEndToEnd = (newModel.getEnd() - lastModel.getEnd()) * 1000;
                double pixelDifference =    millisFromEndToEnd / millisPerPixel;
                if (pixelDifference < 5){
                    lastModel.setEnd(newModel.getEnd());
                    return;
                }
            }
        }
        list.add(newModel);

    }

    public abstract void addToDeclaredChannelMappings(ApiKey apiKey, List<ChannelMapping> mappings);

}
