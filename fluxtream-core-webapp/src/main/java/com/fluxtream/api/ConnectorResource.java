package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.ScheduledUpdate;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/guest/{username}/connector")
@Component("connectorApi")
@Scope("request")
public class ConnectorResource {

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService sysService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    private ApiDataService apiDataService;

    Gson gson = new Gson();

    @GET
    @Path("/all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectors(@PathParam("username") String username){
        Guest user = guestService.getGuest(username);
        if (ControllerHelper.getGuestId() != user.getId()) {
            return gson.toJson(new StatusModel(false, "You don't have access to this information."));
        }
        List<ConnectorInfo> connectors =  sysService.getConnectors();
        List<Long> apiKeyIds = new ArrayList<Long>();
        for (int i = 0; i < connectors.size(); i++){
            if (!guestService.hasApiKey(user.getId(), connectors.get(i).getApi())) {
                connectors.remove(i--);
            }
            else {
                ConnectorInfo connector = connectors.get(i);
                Connector conn = Connector.fromValue(connector.api);
                connector.lastSync = getLastSync(user.getId(),conn);
                connector.latestData = getLatestData(user.getId(), conn);
                connector.errors = checkForErrors(user.getId(),conn);
                connector.syncing = checkIfSyncInProgress(user.getId(),conn);
            }
        }
        return gson.toJson(connectors);
    }

    private boolean checkIfSyncInProgress(long guestId, Connector connector){
        boolean syncing = false;
        for (int objectType : connector.objectTypeValues()){
            ScheduledUpdate update = connectorUpdateService.getNextScheduledUpdate(guestId,connector,objectType);
            syncing = update != null && update.status == ScheduledUpdate.Status.IN_PROGRESS;
            if (syncing)
                break;
        }
        return syncing;
    }

    private boolean checkForErrors(long guestId, Connector connector){
        ApiUpdate update = connectorUpdateService.getLastUpdate(guestId,connector);
        return update == null || !update.success;
    }

    private long getLastSync(long guestId, Connector connector){
        ApiUpdate update = connectorUpdateService.getLastSuccessfulUpdate(guestId, connector);
        return update != null ? update.ts : Long.MAX_VALUE;

    }

    private long getLatestData(long guestId, Connector connector){
        AbstractFacet facet = apiDataService.getLatestApiDataFacet(guestId,connector,null);
        return facet == null ? Long.MAX_VALUE : facet.end;
    }

    @DELETE
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteConnector(@PathParam("username") String username, @PathParam("connector") String connector){
        Guest user = guestService.getGuest(username);
        if (ControllerHelper.getGuestId() != user.getId()) {
            return gson.toJson(new StatusModel(false, "You don't have access to this information."));
        }
        StatusModel result;
        try{
            Connector apiToRemove = Connector.fromString(connector);
            guestService.removeApiKey(user.getId(), apiToRemove);
            result = new StatusModel(true,"Successfully removed " + connector + ".");
        }
        catch (Exception e){
            result = new StatusModel(false,"Failed to remove " + connector + ".");
        }
        return gson.toJson(result);
    }

    @POST
    @Path("/{connector}/sync")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnector(@PathParam("username") String username, @PathParam("connector") String connectorName){
        Guest user = guestService.getGuest(username);
        if (ControllerHelper.getGuestId() != user.getId()) {
            return gson.toJson(new StatusModel(false, "You don't have access to this information."));
        }
        StatusModel result;
        Connector connector = Connector.getConnector(connectorName);
        try {
            int[] objectTypeValues = connector.objectTypeValues();
            for (int objectType : objectTypeValues) {
                ScheduledUpdate update = connectorUpdateService.getNextScheduledUpdate(user.getId(),connector,objectType);
                if (update != null) {
                    connectorUpdateService.reScheduleUpdate(update, System.currentTimeMillis(), false);
                }
                else {
                    connectorUpdateService.scheduleUpdate(user.getId(), connectorName, objectType, UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE, System.currentTimeMillis());
                }
            }
            result = new StatusModel(true,"Successfully scheduled update for " + connectorName);
        }
        catch (Exception e){
            result = new StatusModel(false,"Failed to schedule update for " + connectorName);
        }
        return gson.toJson(result);
    }
}
