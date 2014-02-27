package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.SharedConnector;
import com.fluxtream.mvc.models.GuestModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.CoachingService;
import com.fluxtream.services.GuestService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/coaching")
@Component("RESTCoachingController")
@Scope("request")
public class CoachingController {

    @Autowired
    GuestService guestService;

    @Autowired
    CoachingService coachingService;

    @POST
    @Path("/coaches/find")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel findCoach(@FormParam("username") String username) {
        final Guest guest = guestService.getGuest(username);
        final List<Guest> coaches = coachingService.getCoaches(AuthHelper.getGuestId());
        if (coaches.contains(guest))
            return new StatusModel(false, username + " is already in you coaching buddies list");
        if (guest!=null) {
            StatusModel statusModel = new StatusModel(true, "Found user!");
            statusModel.payload = new GuestModel(guest);
            return statusModel;
        } else
            return new StatusModel(false, "No Such User: " + username + ". Please try again.");
    }

    @POST
    @Path("/coachees/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel getCoaches(@PathParam("username") String username){
        if (username.equals("self")) {
            AuthHelper.as(null);
            return new StatusModel(true, "Viewing own data");
        }
        final long guestId = AuthHelper.getGuestId();
        final CoachingBuddy coachee = coachingService.getCoachee(guestId, username);
        if (coachee==null) {
            return new StatusModel(false, "Could not view " + username +
                                          "'s data. Please refresh the page and " +
                                          "check that you still have access to their data.");
        }
        AuthHelper.as(coachee);
        return new StatusModel(true, "Viewing " + guestService.getGuestById(coachee.guestId).getGuestName() + "'s data");
    }

    @DELETE
    @Path("/coaches/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> removeCoach(@PathParam("username") String username) {
        final long guestId = AuthHelper.getGuestId();
        coachingService.removeCoach(guestId, username);
        final List<Guest> coaches = coachingService.getCoaches(guestId);
        final List<GuestModel> guestModels = toGuestModels(coaches);
        return guestModels;
    }

    @POST
    @Path("/coaches/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> addCoach(@PathParam("username") String username) {
        final long guestId = AuthHelper.getGuestId();
        coachingService.addCoach(guestId, username);
        final List<Guest> coaches = coachingService.getCoaches(guestId);
        final List<GuestModel> guestModels = toGuestModels(coaches);
        return guestModels;
    }

    List<GuestModel> toGuestModels(List<Guest> guests) {
        List<GuestModel> models = new ArrayList<GuestModel>();
        for (Guest guest : guests) {
            models.add(new GuestModel(guest));
        }
        return models;
    }

    @GET
    @Path("/coaches/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectorSharingInfo(@PathParam("username") String username) {
        final long guestId = AuthHelper.getGuestId();
        final CoachingBuddy coachingBuddy = coachingService.getCoach(guestId, username);
        final Set<SharedConnector> sharedConnectors = coachingBuddy.sharedConnectors;
        final List<ApiKey> apiKeys = guestService.getApiKeys(guestId);
        JSONObject coach = new JSONObject();
        JSONArray connectors = new JSONArray();
        for (ApiKey apiKey : apiKeys) {
            boolean isShared = false;
            // Make sure this apiKey is valid, skip if not
            if(apiKey==null || apiKey.getConnector()==null || apiKey.getConnector().getName()==null)
                continue;

            final String connectorName = apiKey.getConnector().getName();
            for (SharedConnector sharedConnector : sharedConnectors) {
                if (sharedConnector.connectorName.equals(connectorName)) {
                    isShared = true;
                    break;
                }
            }
            JSONObject connector = new JSONObject();
            connector.accumulate("prettyName", apiKey.getConnector().prettyName());
            connector.accumulate("connectorName", connectorName);
            connector.accumulate("shared", isShared);
            connectors.add(connector);
        }
        coach.accumulate("sharedConnectors", connectors);
        Guest buddyGuest = guestService.getGuest(username);
        coach.accumulate("username", buddyGuest.username);
        coach.accumulate("fullname", buddyGuest.getGuestName());
        return coach.toString();
    }

    @GET
    @Path("/coaches")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> getCoaches(){
        final long guestId = AuthHelper.getGuestId();
        final List<Guest> coaches = coachingService.getCoaches(guestId);
        final List<GuestModel> guestModels = toGuestModels(coaches);
        return guestModels;
    }

    @POST
    @Path("/coaches/{username}/connectors/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel addSharedConnector(@PathParam("username") String username,
                                          @PathParam("connector") String connectorName) {
        coachingService.addSharedConnector(AuthHelper.getGuestId(), username, connectorName, "{}");
        return new StatusModel(true, "Successfully added a connector (" + username + "/" + connectorName + ")");
    }

    @DELETE
    @Path("/coaches/{username}/connectors/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel removeSharedConnector(@PathParam("username") String username,
                                             @PathParam("connector") String connectorName) {
        coachingService.removeSharedConnector(AuthHelper.getGuestId(), username, connectorName);
        return new StatusModel(true, "Successfully removed a connector (" + username + "/" + connectorName + ")");
    }

}
