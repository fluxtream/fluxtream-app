package org.fluxtream.api;

import java.util.Collection;
import java.util.List;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.updaters.ScheduleResult;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.ApiUpdate;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.UpdateWorkerTask;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.ConnectorUpdateService;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.SystemService;
import org.fluxtream.updaters.quartz.Producer;
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
@Scope("request")
public class SyncController {

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService sysService;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    Gson gson = new Gson();

    private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime().withZone(DateTimeZone.forID("UTC"));

    @POST
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnector(@PathParam("connector") String connectorName){
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
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnectorObjectType(@PathParam("connector") String connectorName,
                                            @PathParam("objectTypes") int objectTypes){
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
    @Path("/{connector}/historyComplete")
    public String isHistoryComplete(@PathParam("connector") String connectorName,
                                    @FormParam("objectTypes") int objectTypes) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final boolean historyUpdateCompleted = connectorUpdateService.isHistoryUpdateCompleted(apiKey, objectTypes);
        JSONObject response = new JSONObject();
        response.accumulate("historyUpdateCompleted", historyUpdateCompleted);
        return response.toString();
    }

    @POST
    @Path("/{connector}/isSynching")
    public String isSynching(@PathParam("connector") String connectorName) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final Collection<UpdateWorkerTask> scheduledUpdates = connectorUpdateService.getUpdatingUpdateTasks(apiKey);
        JSONObject response = new JSONObject();
        response.accumulate("synching", scheduledUpdates.size()>0);
        return response.toString();
    }

    @POST
    @Path("/{connector}/lastSuccessfulUpdate")
    public String lastSuccessfulUpdate(@PathParam("connector") String connectorName) {
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
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel resetConnector(@PathParam("connector") String connectorName) {
        final long guestId = AuthHelper.getGuestId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);
        return new StatusModel(true, "reset controller " + connectorName);
    }

    @POST
    @Path("/{connector}/lastUpdate")
    public String lastUpdate(@PathParam("connector") String connectorName) {
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
