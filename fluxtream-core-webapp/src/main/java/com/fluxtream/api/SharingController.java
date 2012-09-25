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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.SharedConnector;
import com.fluxtream.domain.SharingBuddy;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.GuestModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SharingService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/sharing")
@Component("RESTSharingController")
@Scope("request")
public class SharingController {

    @Autowired
    GuestService guestService;

    @Autowired
    SharingService sharingService;

    @POST
    @Path("/buddies/find")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel findUser(@FormParam("username") String username) {
        setTransactionName(null, "POST /sharing/findUser?" + username);
        final Guest guest = guestService.getGuest(username);
        if (guest!=null) {
            StatusModel statusModel = new StatusModel(true, "Found user!");
            statusModel.payload = new GuestModel(guest);
            return statusModel;
        } else
            return new StatusModel(false, "No Such User: " + username + ". Please try again.");
    }

    @DELETE
    @Path("/buddies/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> removeBuddy(@PathParam("username") String username) {
        setTransactionName(null, "DELETE /sharing/buddies/" + username);
        final long guestId = ControllerHelper.getGuestId();
        sharingService.removeSharingBuddy(guestId,
                                          username);
        final List<Guest> buddies = sharingService.getBuddies(guestId);
        final List<GuestModel> guestModels = toGuestModels(buddies);
        return guestModels;
    }

    @POST
    @Path("/buddies/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> addBuddy(@PathParam("username") String username) {
        setTransactionName(null, "POST /sharing/buddies/" + username);
        final long guestId = ControllerHelper.getGuestId();
        sharingService.addSharingBuddy(guestId, username);
        final List<Guest> buddies = sharingService.getBuddies(guestId);
        final List<GuestModel> guestModels = toGuestModels(buddies);
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
    @Path("/buddies/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectorSharingInfo(@PathParam("username") String username) {
        setTransactionName(null, "GET /sharing/" + username + "/connectors");
        final long guestId = ControllerHelper.getGuestId();
        final SharingBuddy sharingBuddy = sharingService.getSharingBuddy(guestId, username);
        final Set<SharedConnector> sharedConnectors = sharingBuddy.sharedConnectors;
        final List<ApiKey> apiKeys = guestService.getApiKeys(guestId);
        JSONObject buddy = new JSONObject();
        JSONArray connectors = new JSONArray();
        for (ApiKey apiKey : apiKeys) {
            boolean isShared = false;
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
        buddy.accumulate("sharedConnectors", connectors);
        Guest buddyGuest = guestService.getGuest(username);
        buddy.accumulate("username", buddyGuest.username);
        buddy.accumulate("fullname", buddyGuest.getGuestName());
        return buddy.toString();
    }

    @GET
    @Path("/buddies")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> getBuddies(){
        setTransactionName(null, "GET /sharing/buddies");
        final long guestId = ControllerHelper.getGuestId();
        final List<Guest> buddies = sharingService.getBuddies(guestId);
        final List<GuestModel> guestModels = toGuestModels(buddies);
        return guestModels;
    }

    @POST
    @Path("/buddies/{username}/connectors/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel addSharedConnector(@PathParam("username") String username,
                                          @PathParam("connector") String connectorName) {
        setTransactionName(null, "POST /sharing/" + username + "/addSharedConnector");
        sharingService.addSharedConnector(ControllerHelper.getGuestId(), username, connectorName, "{}");
        return new StatusModel(true, "Successfully added a connector (" + username + "/" + connectorName + ")");
    }

    @DELETE
    @Path("/buddies/{username}/connectors/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel removeSharedConnector(@PathParam("username") String username,
                                             @PathParam("connector") String connectorName) {
        setTransactionName(null, "POST /sharing/" + username + "/removeSharedConnector");
        sharingService.removeSharedConnector(ControllerHelper.getGuestId(), username, connectorName);
        return new StatusModel(true, "Successfully removed a connector (" + username + "/" + connectorName + ")");
    }

}
