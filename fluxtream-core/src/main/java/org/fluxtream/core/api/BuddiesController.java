package org.fluxtream.core.api;

import com.sun.jersey.api.Responses;
import com.wordnik.swagger.annotations.*;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.SharedConnectorSettingsAwareUpdater;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.mvc.models.CoachModel;
import org.fluxtream.core.mvc.models.GuestModel;
import org.fluxtream.core.mvc.models.SharedChannelModel;
import org.fluxtream.core.mvc.models.SharedConnectorModel;
import org.fluxtream.core.services.BodyTrackStorageService;
import org.fluxtream.core.services.BuddiesService;
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
@Path("/v1/buddies")
@Api(value = "/buddies", description = "Data sharing")
@Component("RESTBuddiesController")
@Scope("request")
public class BuddiesController {

    @Autowired
    GuestService guestService;

    @Autowired
    BuddiesService buddiesService;

    @Autowired
    BodyTrackStorageService bodyTrackStorageService;

    @Autowired
    BeanFactory beanFactory;

    @POST
    @Path("/find")
    @ApiOperation(value = "Find a buddy", response = GuestModel.class)
    @ApiResponses({
            @ApiResponse(code=400, message="The user is already trusted by the calling guest"),
            @ApiResponse(code=404, message="No such user was found")
    })
    @Produces({MediaType.APPLICATION_JSON})
    public Response findCoach(@ApiParam(value="The buddy's username", required=true) @FormParam("username") String username) {
        final Guest guest = guestService.getGuest(username);
        final List<Guest> coaches = buddiesService.getTrustedBuddies(AuthHelper.getGuestId());
        if (coaches.contains(guest))
            return Response.status(Response.Status.BAD_REQUEST).entity(username + " is already in you coaching buddies list").build();
        if (guest!=null) {
            return Response.ok(new GuestModel(guest)).build();
        } else
            return Responses.notFound().entity("No Such User: " + username + ". Please try again.").build();
    }

    @DELETE
    @Path("/trusted/{username}")
    @ApiOperation(value = "Remove a buddy, revoking all access to the calling guest's data",responseContainer = "Array",
            response = GuestModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> removeCoach(@ApiParam(value="The buddy's username", required=true) @PathParam("username") String username) {
        final long guestId = AuthHelper.getGuestId();
        buddiesService.removeTrustedBuddy(guestId, username);
        final List<Guest> coaches = buddiesService.getTrustedBuddies(guestId);
        final List<GuestModel> guestModels = toGuestModels(coaches);
        return guestModels;
    }

    @POST
    @Path("/trusted/{username}")
    @ApiOperation(value = "Add a buddy to whom we are now able to allow access to the calling guest's connectors",
            response = GuestModel.class, responseContainer = "Array")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> addCoach(@ApiParam(value="The buddy's username", required=true) @PathParam("username") String username) {
        final long guestId = AuthHelper.getGuestId();
        buddiesService.addTrustedBuddy(guestId, username);
        final List<Guest> coaches = buddiesService.getTrustedBuddies(guestId);
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

    @PUT
    @Path("/trusted/{username}/channels/{channelId}/shared")
    @ApiOperation(value = "Retrieve information about data shared with a given buddy")
    public Response setChannelSharingStatus(@ApiParam(value="The buddy's username", required=true) @PathParam("username") String username,
                                            @ApiParam(value="The channel ID", required=true) @PathParam("channelId") long channelId,
                                            @ApiParam(value="Share the channel?", required=true) @FormParam("value") boolean shared) {
        final long trustingBuddyId = AuthHelper.getGuestId();
        final TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(trustingBuddyId, username);
        if (trustedBuddy ==null)
            return Response.status(Response.Status.NOT_FOUND).entity("Could not buddy with username '" + username + "'").build();
        if (!shared) {
            List<SharedChannel> sharedChannels = buddiesService.getSharedChannels(trustedBuddy.buddyId, trustingBuddyId);
            boolean foundChannel = false;
            for (SharedChannel sharedChannel : sharedChannels) {
                if (sharedChannel.channelMapping.getId() == channelId) {
                    foundChannel = true;
                    break;
                }
            }
            if (!foundChannel)
                return Response.status(Response.Status.NOT_FOUND).entity("Could not find channel [" + channelId + "]").build();
            buddiesService.removeSharedChannel(trustedBuddy.buddyId, trustingBuddyId, channelId);
        } else
            buddiesService.addSharedChannel(trustedBuddy.buddyId, trustingBuddyId, channelId);
        return Response.ok().build();
    }


    @GET
    @Path("/trusted/{username}/connectors")
    @ApiOperation(value = "Retrieve information about data shared with a given buddy",
            response = CoachModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public CoachModel getConnectorSharingInfo(@ApiParam(value="The buddy's username", required=true) @PathParam("username") String username) {
        final long trustingBuddyId = AuthHelper.getGuestId();
        final TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(trustingBuddyId, username);
        final Set<SharedConnector> sharedConnectors = trustedBuddy.sharedConnectors;
        final List<ApiKey> apiKeys = guestService.getApiKeys(trustingBuddyId);
        CoachModel coach = new CoachModel();
        Guest trustedBuddyGuest = guestService.getGuest(username);
        coach.username = trustedBuddyGuest.username;
        coach.fullname = trustedBuddyGuest.getGuestName();
        addSharedConnectors(sharedConnectors, apiKeys, coach);
        addSharedChannels(apiKeys, coach, trustingBuddyId, trustedBuddyGuest.getId());
        return coach;
    }

    private void addSharedChannels(List<ApiKey> apiKeys, CoachModel coach, long trustingBuddyId, long trustedBuddyId) {
        for (ApiKey apiKey : apiKeys) {
            // get all availabale channel mappings
            List<ChannelMapping> channelMappings = bodyTrackStorageService.getChannelMappings(apiKey.getId());
            // now those that are shared
            List<SharedChannel> sharedChannels = buddiesService.getSharedChannels(trustedBuddyId, trustingBuddyId, apiKey.getId());
            // create the list of models that we're going to add items to
            List<SharedChannelModel> sharedChannelModels = new ArrayList<SharedChannelModel>();
            for (ChannelMapping channelMapping : channelMappings) {
                // create a SharedChannelModel for each Channel Mapping – shared is false by default
                SharedChannelModel sharedChannelModel = new SharedChannelModel();
                sharedChannelModel.channelId = channelMapping.getId();
                sharedChannelModel.deviceName = channelMapping.getDeviceName().trim();
                sharedChannelModel.channelName = channelMapping.getChannelName().trim();
                if (channelMapping.getInternalDeviceName()!=null&&!channelMapping.getInternalDeviceName().equals(channelMapping.getDeviceName())) {
                    sharedChannelModel.userData = true;
                    sharedChannelModel.deviceName = channelMapping.getInternalDeviceName().trim();
                }
                // iterate through sharedChannels to check if it is shared
                for (SharedChannel sharedChannel : sharedChannels) {
                    if (sharedChannel.channelMapping.getId().equals(channelMapping.getId())) {
                        sharedChannelModel.shared = true;
                        break;
                    }
                }
                if (sharedChannelModel.userData) {
                    List<SharedChannelModel> userDataChannels = coach.sharedChannels.get(sharedChannelModel.deviceName);
                    if (userDataChannels == null) {
                        userDataChannels = new ArrayList<SharedChannelModel>();
                        coach.sharedChannels.put(sharedChannelModel.deviceName, userDataChannels);
                    }
                    userDataChannels.add(sharedChannelModel);
                } else
                    sharedChannelModels.add(sharedChannelModel);
            }
            // finally add an entry to our coach model's sharedChannels map
            coach.sharedChannels.put(apiKey.getConnector().getDeviceNickname(), sharedChannelModels);
        }
    }

    private void addSharedConnectors(Set<SharedConnector> sharedConnectors, List<ApiKey> apiKeys, CoachModel coach) {
        List<SharedConnectorModel> connectors = new ArrayList<SharedConnectorModel>();
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
            SharedConnectorModel connector = new SharedConnectorModel();
            connector.prettyName = apiKey.getConnector().prettyName();
            connector.connectorName = connectorName;
            connector.shared = isShared;
            connector.apiKeyId = apiKey.getId();
            if (SharedConnectorSettingsAwareUpdater.class.isAssignableFrom(apiKey.getConnector().getUpdaterClass()))
                connector.hasSettings = true;
            connectors.add(connector);
        }
        coach.sharedConnectors  = connectors;
    }

    @GET
    @Path("/trusted")
    @ApiOperation(value = "Retrieve the list of buddies whose data we may have access to",
            response = GuestModel.class, responseContainer = "Array")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> getTrustedBuddies(){
        Guest guest = AuthHelper.getGuest();
        final List<Guest> trustedBuddies = buddiesService.getTrustedBuddies(guest.getId());
        final List<GuestModel> guestModels = toGuestModels(trustedBuddies);
        return guestModels;
    }

    @GET
    @Path("/trusting")
    @ApiOperation(value = "Retrieve the list of buddies with whom the calling guest may have shared data",
            response = GuestModel.class, responseContainer = "Array")
    @Produces({MediaType.APPLICATION_JSON})
    public List<GuestModel> getTrustingBuddies(){
        final long guestId = AuthHelper.getGuestId();
        final List<Guest> trustingBuddies = buddiesService.getTrustingBuddies(guestId);
        final List<GuestModel> guestModels = toGuestModels(trustingBuddies);
        return guestModels;
    }

    @POST
    @Path("/trusted/{username}/connectors/{connector}")
    @ApiOperation(value = "Share a connector with a buddy")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully added a connector ({username}/{connectorName})")})
    public Response addSharedConnector(@ApiParam(value="The buddy's username", required=true) @PathParam("username") String username,
                                       @ApiParam(value="A connector name", required=true) @PathParam("connector") String connectorName) {
        final SharedConnector sharedConnector = buddiesService.addSharedConnector(AuthHelper.getGuestId(), username, connectorName, "{}");
        final ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector(connectorName));
        final Class<? extends AbstractUpdater> updaterClass = apiKey.getConnector().getUpdaterClass();
        if (SharedConnectorSettingsAwareUpdater.class.isAssignableFrom(updaterClass)) {
            final SharedConnectorSettingsAwareUpdater updater = (SharedConnectorSettingsAwareUpdater) beanFactory.getBean(updaterClass);
            updater.syncSharedConnectorSettings(apiKey.getId(), sharedConnector);
        }
        return Response.ok("Successfully added a connector (" + username + "/" + connectorName + ")").build();
    }

    @DELETE
    @Path("/trusted/{username}/connectors/{connector}")
    @ApiOperation(value = "Stop sharing a connector with a buddy")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successfully removed a connector ({username}/{connectorName})")})
    public Response removeSharedConnector(@ApiParam(value="The buddy's username", required=true) @PathParam("username") String username,
                                          @ApiParam(value="A connector name", required=true) @PathParam("connector") String connectorName) {
        buddiesService.removeSharedConnector(AuthHelper.getGuestId(), username, connectorName);
        return Response.ok().entity("Successfully removed a connector (" + username + "/" + connectorName + ")").build();
    }

    @GET
    @Path("/trusted/sharedConnector/{apiKeyId}/{username}")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Retrieve sharing details for a given connector", notes= "Note: the structure of the returned object is connector specific")
    public String getSharedConnectorSettings(@ApiParam(value="ID of a connector instance", required=true) @PathParam("apiKeyId") long apiKeyId,
                                             @ApiParam(value="The buddy's username", required=true) @PathParam("username") String username) {
        final long buddyId = guestService.getGuest(username).getId();
        final SharedConnector sharedConnector = buddiesService.getSharedConnector(apiKeyId, buddyId);
        return sharedConnector.filterJson;
    }

    @POST
    @Path("/trusted/sharedConnector/{apiKeyId}/{username}")
    @ApiOperation(value = "Specify sharing details for a given connector – the structure of the specification object is connector specific")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Saved shared connector filter object")})
    public Response saveSharedConnectorSettingsFilter(@ApiParam(value="ID of a connector instance", required=true) @PathParam("apiKeyId") long apiKeyId,
                                                      @ApiParam(value="The buddy's username", required=true) @PathParam("username") String username,
                                                      @ApiParam(value="Custom connector sharing specification", required=true) @FormParam("json") String json) {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final long guestId = AuthHelper.getGuestId();
        final long buddyId = guestService.getGuest(username).getId();
        try {
            if (apiKey.getGuestId()!=guestId)
                throw new RuntimeException("attempt to retrieve ApiKey from another guest!");
            final SharedConnector sharedConnector = buddiesService.getSharedConnector(apiKeyId, buddyId);
            buddiesService.setSharedConnectorFilter(sharedConnector.getId(), json);
        } catch (Throwable e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok("Saved shared connector filter object").build();
    }

}
