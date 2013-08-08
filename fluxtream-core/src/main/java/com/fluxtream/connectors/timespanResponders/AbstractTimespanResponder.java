package com.fluxtream.connectors.timespanResponders;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.mvc.models.TimespanModel;
import com.fluxtream.services.ApiDataService;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractTimespanResponder {

    protected static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    protected List<AbstractFacet> getFacetsInTimespan(ApiDataService apiDataService,TimeInterval timeInterval, ApiKey apiKey, ObjectType objectType){
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


    public abstract List<TimespanModel> getTimespans(long startMillis, long endMillis, ApiKey apiKey, String channelName, ApiDataService apiDataService);
}
