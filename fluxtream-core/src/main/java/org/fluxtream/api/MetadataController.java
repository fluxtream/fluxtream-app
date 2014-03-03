package org.fluxtream.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.fluxtream.Configuration;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.domain.metadata.FoursquareVenue;
import org.fluxtream.domain.metadata.VisitedCity;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.mvc.models.VisitedCityModel;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.MetadataService;
import org.fluxtream.utils.TimeUtils;
import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * User: candide
 * Date: 05/06/13
 * Time: 12:07
 */
@Path("/metadata")
@Component("RESTMetadataController")
@Scope("request")
public class MetadataController {

    FlxLogger logger = FlxLogger.getLogger(MetadataController.class);

    @Autowired
    MetadataService metadataService;

    @Autowired
    GuestService guestService;

    @Autowired
    Configuration env;

    Gson gson = new Gson();

    @POST
    @Path(value="/mainCity/date/{date}")
    @Produces({ MediaType.APPLICATION_JSON } )
    public StatusModel setDayMainCity(@FormParam("latitude") float latitude,
                                      @FormParam("longitude") float longitude,
                                      @PathParam("date") String date) {
        final long guestId = AuthHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDayMainCity")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        metadataService.setDayMainCity(guestId, latitude, longitude, date);
        return new StatusModel(true, "OK");
    }

    @DELETE
    @Path(value="/mainCity/date/{date}")
    @Produces({ MediaType.APPLICATION_JSON } )
    public StatusModel resetDayMainCity(@PathParam("date") String date) {
        final long guestId = AuthHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=resetDayMainCity")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        metadataService.resetDayMainCity(guestId, date);
        return new StatusModel(true, "OK");
    }

    @POST
    @Path(value="/mainCity/{visitedCityId}/date/{date}")
    @Produces({ MediaType.APPLICATION_JSON } )
    public StatusModel setDayMainCity(@PathParam("visitedCityId") long visitedCityId,
                                      @PathParam("date") String date) {
        final long guestId = AuthHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDayMainCity")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        metadataService.setDayMainCity(guestId, visitedCityId, date);
        return new StatusModel(true, "OK");
    }

    @GET
    @Path(value="/foursquare/venue/{venueId}")
    @Produces({ MediaType.APPLICATION_JSON } )
    public Response getFoursquareVenue(@PathParam("venueId") String venueId) {
        // this doesn't seem to have any effect, i.e. the cache-control header is alwasy set to no-cache
        // needs invistigating...
        CacheControl cc = new CacheControl();
        // cache for a month
        cc.setNoCache(false);
        cc.setMaxAge(86400*31);
        final FoursquareVenue foursquareVenue = metadataService.getFoursquareVenue(venueId);
        Response.ResponseBuilder builder = Response.ok(foursquareVenue);
        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path(value = "/checkIn/{ipAddress}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel checkIn(@PathParam("ipAddress") String ipAddress){
        final long guestId = AuthHelper.getGuestId();
        try {
            guestService.checkIn(guestId, ipAddress);
            return new StatusModel(true, "Guest successfully checked in");
        }
        catch (IOException e) {
            return new StatusModel(false, "Unexpected error while checking in: " + e.getMessage());
        }
    }

    @GET
    @Path(value = "/cities")
    @Produces({MediaType.APPLICATION_JSON})
    public String getCitiesForRange(@QueryParam("start") long start, @QueryParam("end") long end){
        TreeSet<String> dates = new TreeSet<String>();
        DateTime startDate = new DateTime(start);
        DateTime endDate = new DateTime(end);
        while (startDate.isBefore(endDate)){
            dates.add(TimeUtils.dateFormatter.print(startDate));
            startDate = startDate.plusDays(1);
        }
        String finalDate = TimeUtils.dateFormatter.print(endDate);
        if (!dates.contains(finalDate)) dates.add(finalDate);
        List<VisitedCityModel> cities = new ArrayList<VisitedCityModel>();
        for (VisitedCity city : metadataService.getConsensusCities(AuthHelper.getGuestId(), dates)){
            cities.add(new VisitedCityModel(city,env));
        }
        return gson.toJson(cities);
    }

}
