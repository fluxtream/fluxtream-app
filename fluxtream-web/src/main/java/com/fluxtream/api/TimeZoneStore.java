package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.mvc.models.TimeZoneMappingModel;
import com.fluxtream.mvc.models.TimeZoneSegmentModel;
import com.fluxtream.services.MetadataService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

@Path("/timezones")
@Component("RESTTimeZoneStore")
@Scope("request")
public class TimeZoneStore {

    @Autowired
    MetadataService metadataService;

    Gson gson = new Gson();

    @GET
    @Path("/mapping")
    @Produces({MediaType.APPLICATION_JSON})
    public String getTimeZoneMapping(){
        setTransactionName(null, "GET /timezones/mapping");
        TimeZoneMappingModel timeZoneMapping = new TimeZoneMappingModel();
        timeZoneMapping.timeZones = new ArrayList<TimeZoneSegmentModel>();
        List<DayMetadataFacet> metaData = metadataService.getAllDayMetadata(AuthHelper.getGuestId());
        for (DayMetadataFacet dayMetadata  : metaData){
            TimeZoneSegmentModel curTimeZone;
            if (timeZoneMapping.timeZones.size() == 0 || !timeZoneMapping.timeZones.get(timeZoneMapping.timeZones.size() - 1).name.equals(dayMetadata.timeZone)){
                curTimeZone = new TimeZoneSegmentModel();
                curTimeZone.name = dayMetadata.timeZone;
                curTimeZone.start = dayMetadata.start;
                curTimeZone.tz = TimeZone.getTimeZone(curTimeZone.name);
                curTimeZone.start +=  curTimeZone.tz.getOffset(curTimeZone.start);
                curTimeZone.offset = curTimeZone.tz.getRawOffset();
                curTimeZone.usesDST = curTimeZone.tz.useDaylightTime();
                timeZoneMapping.timeZones.add(curTimeZone);
            }
            else{
                curTimeZone = timeZoneMapping.timeZones.get(timeZoneMapping.timeZones.size() - 1);
            }
            curTimeZone.end = dayMetadata.end;
            curTimeZone.end += curTimeZone.tz.getOffset(curTimeZone.end);
        }
        return gson.toJson(timeZoneMapping);
    }
}
