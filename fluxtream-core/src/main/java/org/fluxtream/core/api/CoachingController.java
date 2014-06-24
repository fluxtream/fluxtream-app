package org.fluxtream.core.api;

import com.sun.jersey.api.Responses;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.SharedConnectorSettingsAwareUpdater;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.SharedConnector;
import org.fluxtream.core.mvc.models.GuestModel;
import org.fluxtream.core.services.CoachingService;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/coaching")
@Component("RESTCoachingController")
@Scope("request")
public class CoachingController {

    @Autowired
    GuestService guestService;

    @Autowired
    CoachingService coachingService;

    @Autowired
    BeanFactory beanFactory;

    @POST
    @Path("/coaches/find")
    @Produces({MediaType.APPLICATION_JSON})
    public Response findCoach(@FormParam("username") String username) {
        final Guest guest = guestService.getGuest(username);
        final List<Guest> coaches = coachingService.getCoaches(AuthHelper.getGuestId());
        if (coaches.contains(guest))
            return Response.status(Response.Status.BAD_REQUEST).entity(username + " is already in you coaching buddies list").build();
        if (guest!=null) {
            return Response.ok(new GuestModel(guest)).build();
        } else
            return Responses.notFound().entity("No Such User: " + username + ". Please try again.").build();
    }

    @POST
    @Path("/coachees/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCoaches(@PathParam("username") String username){
        if (username.equals("self")) {
            AuthHelper.as(null);
            return Response.ok("Viewing own data").build();
        }
        final long guestId = AuthHelper.getGuestId();
        final CoachingBuddy coachee = coachingService.getCoachee(guestId, username);
        if (coachee==null) {
            return Response.status(403).entity("Could not view " + username +
                                          "'s data. Please refresh the page and " +
                                          "check that you still have access to their data.").build();
        }
        AuthHelper.as(coachee);
        return Response.ok("Viewing " + guestService.getGuestById(coachee.guestId).getGuestName() + "'s data").build();
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
            connector.accumulate("apiKeyId", apiKey.getId());
            if (SharedConnectorSettingsAwareUpdater.class.isAssignableFrom(apiKey.getConnector().getUpdaterClass()))
                connector.accumulate("hasSettings", true);
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
    public Response addSharedConnector(@PathParam("username") String username,
                                       @PathParam("connector") String connectorName) {
        final SharedConnector sharedConnector = coachingService.addSharedConnector(AuthHelper.getGuestId(), username, connectorName, "{}");
        final ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector(connectorName));
        final Class<? extends AbstractUpdater> updaterClass = apiKey.getConnector().getUpdaterClass();
        if (SharedConnectorSettingsAwareUpdater.class.isAssignableFrom(updaterClass)) {
            final SharedConnectorSettingsAwareUpdater updater = (SharedConnectorSettingsAwareUpdater) beanFactory.getBean(updaterClass);
            updater.syncSharedConnectorSettings(apiKey.getId(), sharedConnector);
        }
        return Response.ok("Successfully added a connector (" + username + "/" + connectorName + ")").build();
    }

    @DELETE
    @Path("/coaches/{username}/connectors/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response removeSharedConnector(@PathParam("username") String username,
                                          @PathParam("connector") String connectorName) {
        coachingService.removeSharedConnector(AuthHelper.getGuestId(), username, connectorName);
        return Response.ok().entity("Successfully removed a connector (" + username + "/" + connectorName + ")").build();
    }

    @GET
    @Path("/sharedConnector/{apiKeyId}/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getSharedConnectorSettings(@PathParam("apiKeyId") long apiKeyId,
                                             @PathParam("username") String username) {
        final long buddyId = guestService.getGuest(username).getId();
        final SharedConnector sharedConnector = coachingService.getSharedConnector(apiKeyId, buddyId);
        return sharedConnector.filterJson;
    }

    @POST
    @Path("/sharedConnector/{apiKeyId}/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveSharedConnectorSettingsFilter(@PathParam("apiKeyId") long apiKeyId,
                                                         @PathParam("username") String username,
                                                         @FormParam("json") String json) {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final long guestId = AuthHelper.getGuestId();
        final long buddyId = guestService.getGuest(username).getId();
        try {
            if (apiKey.getGuestId()!=guestId)
                throw new RuntimeException("attempt to retrieve ApiKey from another guest!");
            final SharedConnector sharedConnector = coachingService.getSharedConnector(apiKeyId, buddyId);
            coachingService.setSharedConnectorFilter(sharedConnector.getId(), json);
        } catch (Throwable e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok("saved shared connector filter object").build();
    }

}
