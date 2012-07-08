package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.ScheduledUpdate;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.services.SystemService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component("RESTConnectorController")
@Scope("request")
public class ConnectorController {

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService sysService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    private ApiDataService apiDataService;

    Gson gson = new Gson();

    @POST
    @Path("/connectors/{connector}/sync")
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnector(@PathParam("connector") String connectorName){
        Guest user = ControllerHelper.getGuest();
        Connector connector = Connector.getConnector(connectorName);
        return gson.toJson(updateConnector(user, connector));
    }

    @POST
    @Path("/connectors/all/sync")
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
                    UpdateInfo.UpdateType updateType = connectorUpdateService.isHistoryUpdateCompleted(user.getId(),connector.getName(),objectType) ? UpdateInfo.UpdateType.INCREMENTAL_UPDATE : UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE;
                    connectorUpdateService.scheduleUpdate(user.getId(), connector.getName(), objectType, updateType, System.currentTimeMillis());
                }
            }
            return new StatusModel(true,"Successfully scheduled update for " + connector.getName());
        }
        catch (Exception e){
            return new StatusModel(false,"Failed to schedule update for " + connector.getName());
        }
    }
}
