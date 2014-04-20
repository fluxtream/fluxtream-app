package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.domain.metadata.FoursquareVenue;
import org.fluxtream.core.domain.metadata.VisitedCity;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.mvc.models.VisitedCityModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.utils.TimeUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * User: candide
 * Date: 05/06/13
 * Time: 12:07
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Location/Timezone query and disambiguation endpoints.",
        authorizations = {@Authorization(value="oauth2")})
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
    @ApiOperation(value = "Set the main city for a given day using lat/lon coordinates.", response = StatusModel.class,
            notes="(we figure out the actual city from the coordinates)",
            authorizations = {@Authorization(value="oauth2")})
    @Produces({ MediaType.APPLICATION_JSON } )
    public StatusModel setDayMainCity(@ApiParam(value="Latitude", required=true) @FormParam("latitude") float latitude,
                                      @ApiParam(value="Longitude", required=true) @FormParam("longitude") float longitude,
                                      @ApiParam(value="Date (YYYY-MM-DD)", required=true) @PathParam("date") String date) {
        final long guestId = AuthHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDayMainCity")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        metadataService.setDayMainCity(guestId, latitude, longitude, date);
        return new StatusModel(true, "OK");
    }

    @DELETE
    @Path(value="/mainCity/date/{date}")
    @ApiOperation(value = "Remove cities that have been manually entered by the end-user.", response = StatusModel.class,
            authorizations = {@Authorization(value="oauth2")})
    @Produces({ MediaType.APPLICATION_JSON } )
    public StatusModel resetDayMainCity(@ApiParam(value="Date (YYYY-MM-DD)", required=true) @PathParam("date") String date) {
        final long guestId = AuthHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=resetDayMainCity")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        metadataService.resetDayMainCity(guestId, date);
        return new StatusModel(true, "OK");
    }

    @POST
    @Path(value="/mainCity/{visitedCityId}/date/{date}")
    @ApiOperation(value = "Set a given city and associated timezone to be the reference for a given day.", response = StatusModel.class)
    @Produces({ MediaType.APPLICATION_JSON } )
    public StatusModel setDayMainCity(@ApiParam(value="ID of the city (as in /metadata/cities)", required=true) @PathParam("visitedCityId") long visitedCityId,
                                      @ApiParam(value="Date (YYYY-MM-DD)", required=true) @PathParam("date") String date) {
        final long guestId = AuthHelper.getGuestId();
        StringBuilder sb = new StringBuilder("module=API component=calendarController action=setDayMainCity")
                .append(" guestId=").append(guestId);
        logger.info(sb.toString());
        metadataService.setDayMainCity(guestId, visitedCityId, date);
        return new StatusModel(true, "OK");
    }

    @GET
    @Path(value="/foursquare/venue/{venueId}")
    @ApiOperation(value = "Retrieve the Foursquare info for a given venue ID (results are cached)", response = FoursquareVenue.class)
    @Produces({ MediaType.APPLICATION_JSON } )
    public Response getFoursquareVenue(@ApiParam(value="Foursquare venue ID", required=true) @PathParam("venueId") String venueId) {
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
    @ApiOperation(value = "Use ip2location lookup to guess the user's location based on his IP address", response = StatusModel.class,
        notes="The resulting location will interpreted as a place the user was at at that moment")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel checkIn(@ApiParam(value="The end-users terminal IP address", required=true) @PathParam("ipAddress") String ipAddress){
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
    @ApiOperation(value = "The list of cities visited by the user during a given time interval", responseContainer = "Array",
            response = VisitedCityModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public String getCitiesForRange(@ApiParam(value="Start of given time interval", required=true) @QueryParam("start") long start,
                                    @ApiParam(value="End of given time interval", required=true) @QueryParam("end") long end){
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
