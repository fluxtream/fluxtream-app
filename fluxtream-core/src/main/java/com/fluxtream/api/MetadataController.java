package com.fluxtream.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.metadata.FoursquareVenue;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.MetadataService;
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

}
