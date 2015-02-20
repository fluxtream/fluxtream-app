package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wordnik.swagger.annotations.ApiParam;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.TrustRelationshipRevokedException;
import org.fluxtream.core.domain.TrustedBuddy;
import org.fluxtream.core.domain.DataUpdate;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.DataUpdateDigestModel;
import org.fluxtream.core.services.BuddiesService;
import org.fluxtream.core.services.DataUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SettingsService;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/v1/dataUpdates")
@Component("DataUpdateStore")
@Scope("request")
public class DataUpdateStore {

    Gson gson = new GsonBuilder().create();

    @Autowired
    GuestService guestService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    DataUpdateService dataUpdateService;

    @Autowired
    BuddiesService buddiesService;

    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDataUpdates(@QueryParam("since") String since,
                                   @ApiParam(value="Buddy to access username parameter (" + BuddiesService.BUDDY_TO_ACCESS_PARAM + ")", required=false) @QueryParam(BuddiesService.BUDDY_TO_ACCESS_PARAM) String buddyToAccessParameter){
        try{
            TrustedBuddy trustedBuddy;
            try { trustedBuddy = AuthHelper.getBuddyTrustedBuddy(buddyToAccessParameter, buddiesService);
            } catch (TrustRelationshipRevokedException e) {return Response.status(403).entity("Sorry, permission to access this data has been revoked. Please reload your browser window").build();}
            Guest guest = ApiHelper.getBuddyToAccess(guestService, trustedBuddy);
            if (guest==null)
                return Response.status(401).entity("You are no longer logged in").build();
            long guestId = guest.getId();
            List<DataUpdate> updates = dataUpdateService.getAllUpdatesSince(guestId, ISODateTimeFormat.dateTime().parseMillis(since));
            return Response.ok(gson.toJson(new DataUpdateDigestModel(updates,guestService,settingsService,ISODateTimeFormat.dateTime().parseMillis(since)))).build();
        }
        catch (Exception e){
            e.printStackTrace();
            return Response.serverError().entity("Failed to fetch updates").build();
        }
    }
}
