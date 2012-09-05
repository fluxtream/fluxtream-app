package com.fluxtream.api;

import java.util.Collection;
import java.util.List;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;
import com.fluxtream.updaters.quartz.Producer;
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
        return sync(connectorName, false);
    }

    private String sync(final String connectorName, final boolean force) {
        try{
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnector(ControllerHelper.getGuestId(),
                                                                                                Connector.getConnector(connectorName),
                                                                                                force);
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
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnectorObjectType(ControllerHelper.getGuestId(),
                                                                                                Connector.getConnector(connectorName),
                                                                                                objectTypes,
                                                                                                force);
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
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateAllConnectors(ControllerHelper.getGuestId());
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
    public String TestUpdate()
    {
        Producer p = new Producer();
        p.scheduleIncrementalUpdates();
        return gson.toJson(null);
    }

    @POST
    @Path("/{connector}/historyComplete")
    public String isHistoryComplete(@PathParam("connector") String connectorName,
                                    @FormParam("objectTypes") int objectTypes) {
        Guest user = ControllerHelper.getGuest();
        final boolean historyUpdateCompleted = connectorUpdateService.isHistoryUpdateCompleted(user.getId(), connectorName, objectTypes);
        JSONObject response = new JSONObject();
        response.accumulate("historyUpdateCompleted", historyUpdateCompleted);
        return response.toString();
    }

    @POST
    @Path("/{connector}/isSynching")
    public String isSynching(@PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = ControllerHelper.getGuest();
        final Collection<UpdateWorkerTask> scheduledUpdates = connectorUpdateService.getUpdatingUpdateTasks(guest.getId(), connector);
        JSONObject response = new JSONObject();
        response.accumulate("synching", scheduledUpdates.size()>0);
        return response.toString();
    }

    @POST
    @Path("/{connector}/lastSuccessfulUpdate")
    public String lastSuccessfulUpdate(@PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = ControllerHelper.getGuest();
        final ApiUpdate lastSuccessfulUpdate = connectorUpdateService.getLastSuccessfulUpdate(guest.getId(), connector);
        JSONObject response = new JSONObject();
        response.accumulate("lastSuccessfulUpdate", lastSuccessfulUpdate!=null
            ? fmt.print(lastSuccessfulUpdate.ts) : "never");
        return response.toString();
    }

    @POST
    @Path("/{connector}/reset")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel resetConnector(@PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = ControllerHelper.getGuest();
        final ApiUpdate lastSuccessfulUpdate = connectorUpdateService.getLastSuccessfulUpdate(guest.getId(), connector);
        connectorUpdateService.deleteScheduledUpdateTasks(guest.getId(), connector, true);
        return new StatusModel(true, "reset controller " + connectorName);
    }

    @POST
    @Path("/{connector}/lastUpdate")
    public String lastFailedUpdate(@PathParam("connector") String connectorName) {
        Connector connector = Connector.getConnector(connectorName);
        Guest guest = ControllerHelper.getGuest();
        final ApiUpdate lastUpdate = connectorUpdateService.getLastUpdate(guest.getId(), connector);
        JSONObject response = new JSONObject();
        response.accumulate("lastUpdate", lastUpdate!=null
                                                    ? fmt.print(lastUpdate.ts) : "never");
        return response.toString();
    }

}
