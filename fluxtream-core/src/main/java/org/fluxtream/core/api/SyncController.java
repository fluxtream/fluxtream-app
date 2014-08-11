package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.*;
import net.sf.json.JSONObject;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.CoachRevokedException;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.ScheduleResult;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.CoachingService;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.updaters.quartz.Producer;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/v1/sync")
@Component("RESTSyncController")
@Api(value = "/sync", description = "Retrieve information about connector state and schedule connector synchronization")
@Scope("request")
public class SyncController {

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService sysService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    CoachingService coachingService;

    Gson gson = new Gson();

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    @POST
    @Path("/{connector}")
    @ApiOperation(value = "Update a connector belonging to either the logged in user or the buddy-to-access specified in the "
            + CoachingService.BUDDY_TO_ACCESS_HEADER + " header",
            response = ScheduleResult.class, responseContainer = "Array")
    @ApiResponses({
            @ApiResponse(code=401, message="The user is no longer logged in"),
            @ApiResponse(code=403, message="Buddy-to-access authorization has been revoked")
    })
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateConnector(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName,
                                    @ApiParam(value="Buddy to access username Header (" + CoachingService.BUDDY_TO_ACCESS_HEADER + ")", required=false) @HeaderParam(CoachingService.BUDDY_TO_ACCESS_HEADER) String coacheeUsernameHeader){
        return sync(connectorName, coacheeUsernameHeader, true);
    }

    private Response sync(final String connectorName, final String coacheeUsernameHeader, final boolean force) {
        try{
            CoachingBuddy coachee;
            try { coachee = AuthHelper.getCoachee(coacheeUsernameHeader, coachingService);
            } catch (CoachRevokedException e) {return Response.status(403).entity("Sorry, permission to access this data has been revoked. Please reload your browser window").build();}
            Guest guest = ApiHelper.getBuddyToAccess(guestService, coachee);
            final long guestId = guest.getId();
            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            guestService.setApiKeyToSynching(apiKey.getId(), true);
            if (apiKey==null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("we don't have an ApiKey for this connector").build();
            }
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnector(apiKey, force);
            return Response.ok(scheduleResults).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to schedule update: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/{connector}/{objectTypes}")
    @ApiOperation(value = "Update a connector's object types belonging to eigher the logged in user or the buddy-to-access specified in the "
            + CoachingService.BUDDY_TO_ACCESS_HEADER + " header",
            response = ScheduleResult.class, responseContainer = "Array")
    @ApiResponses({
            @ApiResponse(code=401, message="The user is no longer logged in"),
            @ApiResponse(code=403, message="Buddy-to-access authorization has been revoked")
    })
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateConnectorObjectType(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName,
                                              @ApiParam(value="Bit mask of object types that have to be updated", required=true) @PathParam("objectTypes") int objectTypes,
                                              @ApiParam(value="Buddy to access username Header (" + CoachingService.BUDDY_TO_ACCESS_HEADER + ")", required=false) @HeaderParam(CoachingService.BUDDY_TO_ACCESS_HEADER) String coacheeUsernameHeader){
        return syncConnectorObjectType(connectorName, coacheeUsernameHeader, objectTypes, false);
    }

    private Response syncConnectorObjectType(final String connectorName, final String coacheeUsernameHeader,
                                             final int objectTypes, final boolean force) {
        try {
            CoachingBuddy coachee;
            try { coachee = AuthHelper.getCoachee(coacheeUsernameHeader, coachingService);
            } catch (CoachRevokedException e) {return Response.status(403).entity("Sorry, permission to access this data has been revoked. Please reload your browser window").build();}
            Guest guest = ApiHelper.getBuddyToAccess(guestService, coachee);
            final long guestId = guest.getId();

            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnectorObjectType(
                    apiKey, objectTypes, force, false);
            return Response.ok(scheduleResults).build();
        }
        catch (Exception e) {
            return Response.serverError().entity("Failed to schedule update: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/all")
    @ApiOperation(value = "Update all of either the logged in guest's connectors, or the buddy-to-access's connectors", response = ScheduleResult.class, responseContainer = "Array")
    @ApiResponses({
            @ApiResponse(code=401, message="The user is no longer logged in"),
            @ApiResponse(code=403, message="Buddy-to-access authorization has been revoked")
    })
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateAllConnectors(@ApiParam(value="Buddy to access username Header (" + CoachingService.BUDDY_TO_ACCESS_HEADER + ")", required=false) @HeaderParam(CoachingService.BUDDY_TO_ACCESS_HEADER) String coacheeUsernameHeader){
        CoachingBuddy coachee;
        try { coachee = AuthHelper.getCoachee(coacheeUsernameHeader, coachingService);
        } catch (CoachRevokedException e) {return Response.status(403).entity("Sorry, permission to access this data has been revoked. Please reload your browser window").build();}
        Guest guest = ApiHelper.getBuddyToAccess(guestService, coachee);
        final long guestId = guest.getId();
        try {
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateAllConnectors(guestId, true);
            return Response.ok(scheduleResults).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to schedule udpates: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/producerTest")
    @Produces({MediaType.APPLICATION_JSON})
    public String TestUpdate() throws InterruptedException {
        Producer p = new Producer();
        p.scheduleIncrementalUpdates();
        return gson.toJson(null);
    }

    @POST
    @ApiOperation(value = "Check if a connector's history update is complete", response = String.class)
    @Path("/{connector}/historyComplete")
    public Response isHistoryComplete(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName,
                                      @ApiParam(value="Bit mask of the connector's object types", required=true) @FormParam("objectTypes") int objectTypes) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final boolean historyUpdateCompleted = connectorUpdateService.isHistoryUpdateCompleted(apiKey, objectTypes);
        JSONObject response = new JSONObject();
        response.accumulate("historyUpdateCompleted", historyUpdateCompleted);
        return Response.ok(response.toString()).build();
    }

    @POST
    @ApiOperation(value = "Check if a connector's currently synching", response = String.class)
    @Path("/{connector}/isSynching")
    public Response isSynching(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final Collection<UpdateWorkerTask> scheduledUpdates = connectorUpdateService.getUpdatingUpdateTasks(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("synching", scheduledUpdates.size()>0);
        return Response.ok(response.toString()).build();
    }

    @POST
    @ApiOperation(value = "Retrieve a connector's last successful update time", response = String.class)
    @Path("/{connector}/lastSuccessfulUpdate")
    public Response lastSuccessfulUpdate(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), connector);
        final ApiUpdate lastSuccessfulUpdate = connectorUpdateService.getLastSuccessfulUpdate(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("lastSuccessfulUpdate", lastSuccessfulUpdate!=null
            ? fmt.print(lastSuccessfulUpdate.ts) : "never");
        return Response.ok(response.toString()).build();
    }

    @POST
    @Path("/{connector}/reset")
    @ApiOperation(value = "Un-schedule pending updates of the given connector", response = StatusModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public Response resetConnector(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);
        return Response.ok("reset controller " + connectorName).build();
    }

    @POST
    @ApiOperation(value = "Retrieve a connector's last attempted update time (successful or failed)", response = String.class)
    @Path("/{connector}/lastUpdate")
    public Response lastUpdate(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), connector);
        final ApiUpdate lastUpdate = connectorUpdateService.getLastUpdate(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("lastUpdate", lastUpdate!=null
                                                    ? fmt.print(lastUpdate.ts) : "never");
        return Response.ok(response.toString()).build();
    }

}
