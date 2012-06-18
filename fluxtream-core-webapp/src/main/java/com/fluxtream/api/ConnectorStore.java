package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
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

/**
 *
 */
@Path("/connectors")
@Component("RESTConnectorStore")
@Scope("request")
public class ConnectorStore {
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
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectors(){
        Guest user = ControllerHelper.getGuest();
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
    public String deleteConnector(@PathParam("connector") String connector){
        Guest user = ControllerHelper.getGuest();
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
    public String updateConnector(@PathParam("connector") String connectorName){
        Guest user = ControllerHelper.getGuest();
        Connector connector = Connector.getConnector(connectorName);
        return gson.toJson(updateConnector(user, connector));
    }

    @POST
    @Path("/all/sync")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateAllConnectors(){
        Guest user = ControllerHelper.getGuest();
        StatusModel result = null;
        List<ConnectorInfo> connectors =  sysService.getConnectors();
        List<Long> apiKeyIds = new ArrayList<Long>();
        for (int i = 0; i < connectors.size(); i++){
            if (!guestService.hasApiKey(user.getId(), connectors.get(i).getApi())) {
                connectors.remove(i--);
            }
            else {
                Connector connector = connectors.get(i).getApi();
                StatusModel updateRes = updateConnector(user, connector);
                if (result == null && updateRes.result.equals("KO"))
                    result = new StatusModel(false,"Some connectors failed to update");
            }
        }
        if (result == null)
            result= new StatusModel(true,"Successfully updated all connectors");
        return gson.toJson(result);
    }

    private StatusModel updateConnector(Guest user, Connector connector){
        try {
            int[] objectTypeValues = connector.objectTypeValues();
            for (int objectType : objectTypeValues) {
                ScheduledUpdate update = connectorUpdateService.getNextScheduledUpdate(user.getId(),connector,objectType);
                if (update != null) {
                    connectorUpdateService.reScheduleUpdate(update, System.currentTimeMillis(), false);
                }
                else {
                    connectorUpdateService.scheduleUpdate(user.getId(), connector.getName(), objectType, UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE, System.currentTimeMillis());
                }
            }
            return new StatusModel(true,"Successfully scheduled update for " + connector.getName());
        }
        catch (Exception e){
            return new StatusModel(false,"Failed to schedule update for " + connector.getName());
        }
    }

}
