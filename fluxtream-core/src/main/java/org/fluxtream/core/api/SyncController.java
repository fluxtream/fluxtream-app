package org.fluxtream.core.api;

import java.util.Collection;
import java.util.List;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.ScheduleResult;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiUpdate;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.UpdateWorkerTask;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.updaters.quartz.Producer;
import com.google.gson.Gson;
import net.sf.json.JSONObject;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Path("/sync")
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

    Gson gson = new Gson();

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    @POST
    @Path("/{connector}")
    @ApiOperation(value = "Update a connector", response = StatusModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnector(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName){
        return sync(connectorName, true);
    }

    private String sync(final String connectorName, final boolean force) {
        try{
            final long guestId = AuthHelper.getGuestId();
            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            guestService.setApiKeyToSynching(apiKey.getId(), true);
            if (apiKey==null) {
                return gson.toJson(new StatusModel(false, "we don't have an ApiKey for this connector"));
            }
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnector(apiKey, force);
            StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
            statusModel.payload = scheduleResults;
            return gson.toJson(scheduleResults);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to schedule update: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{connector}/{objectTypes}")
    @ApiOperation(value = "Update a connector's object types", response = StatusModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnectorObjectType(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName,
                                            @ApiParam(value="Bit mask of object types that have to be updated", required=true) @PathParam("objectTypes") int objectTypes){
        return syncConnectorObjectType(connectorName, objectTypes, false);
    }

    private String syncConnectorObjectType(final String connectorName, final int objectTypes, final boolean force) {
        try {
            final long guestId = AuthHelper.getGuestId();
            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnectorObjectType(
                    apiKey, objectTypes, force, false);
            StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
            statusModel.payload = scheduleResults;
            return gson.toJson(scheduleResults);
        }
        catch (Exception e) {
            return gson.toJson(new StatusModel(false,"Failed to schedule update: " + e.getMessage()));
        }
    }

    @POST
    @Path("/all")
    @ApiOperation(value = "Update all of the logged in guest's connectors", response = StatusModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public String updateAllConnectors(){
        try {
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateAllConnectors(AuthHelper.getGuestId(), true);
            StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
            statusModel.payload = scheduleResults;
            return gson.toJson(scheduleResults);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to schedule udpates: " + e.getMessage()));
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
    public String isHistoryComplete(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName,
                                    @ApiParam(value="Bit mask of the connector's object types", required=true) @FormParam("objectTypes") int objectTypes) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final boolean historyUpdateCompleted = connectorUpdateService.isHistoryUpdateCompleted(apiKey, objectTypes);
        JSONObject response = new JSONObject();
        response.accumulate("historyUpdateCompleted", historyUpdateCompleted);
        return response.toString();
    }

    @POST
    @ApiOperation(value = "Check if a connector's currently synching", response = String.class)
    @Path("/{connector}/isSynching")
    public String isSynching(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final Collection<UpdateWorkerTask> scheduledUpdates = connectorUpdateService.getUpdatingUpdateTasks(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("synching", scheduledUpdates.size()>0);
        return response.toString();
    }

    @POST
    @ApiOperation(value = "Retrieve a connector's last successful update time", response = String.class)
    @Path("/{connector}/lastSuccessfulUpdate")
    public String lastSuccessfulUpdate(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), connector);
        final ApiUpdate lastSuccessfulUpdate = connectorUpdateService.getLastSuccessfulUpdate(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("lastSuccessfulUpdate", lastSuccessfulUpdate!=null
            ? fmt.print(lastSuccessfulUpdate.ts) : "never");
        return response.toString();
    }

    @POST
    @Path("/{connector}/reset")
    @ApiOperation(value = "Un-schedule pending updates of the given connector", response = StatusModel.class)
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel resetConnector(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);
        return new StatusModel(true, "reset controller " + connectorName);
    }

    @POST
    @ApiOperation(value = "Retrieve a connector's last attempted update time (successful or failed)", response = String.class)
    @Path("/{connector}/lastUpdate")
    public String lastUpdate(@ApiParam(value="Connector name", required=true) @PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = AuthHelper.getGuest();
        final ApiKey apiKey = guestService.getApiKey(guest.getId(), connector);
        final ApiUpdate lastUpdate = connectorUpdateService.getLastUpdate(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("lastUpdate", lastUpdate!=null
                                                    ? fmt.print(lastUpdate.ts) : "never");
        return response.toString();
    }

}
