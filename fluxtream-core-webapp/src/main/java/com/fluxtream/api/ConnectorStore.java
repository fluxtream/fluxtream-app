package com.fluxtream.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.services.SystemService;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    SettingsService settingsService;

    @Qualifier("connectorUpdateServiceImpl")
    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Qualifier("apiDataServiceImpl")
    @Autowired
    private ApiDataService apiDataService;

    Gson gson = new Gson();

    @GET
    @Path("/installed")
    @Produces({MediaType.APPLICATION_JSON})
    public String getInstalledConnectors(){
        try{
            Guest user = ControllerHelper.getGuest();
            List<ConnectorInfo> connectors =  sysService.getConnectors();
            JSONArray connectorsArray = new JSONArray();
            for (int i = 0; i < connectors.size(); i++){
                if (!guestService.hasApiKey(user.getId(), connectors.get(i).getApi())) {
                    connectors.remove(i--);
                }
                else {
                    ConnectorInfo connector = connectors.get(i);
                    JSONObject connectorJson = new JSONObject();
                    Connector conn = Connector.fromValue(connector.api);
                    connectorJson.accumulate("name", connector.name);
                    connectorJson.accumulate("connectUrl", connector.connectUrl);
                    connectorJson.accumulate("image", connector.image);
                    connectorJson.accumulate("connectorName", connector.connectorName);
                    connectorJson.accumulate("enabled", connector.enabled);
                    connectorJson.accumulate("manageable", connector.manageable);
                    connectorJson.accumulate("text", connector.text);
                    connectorJson.accumulate("api", connector.api);
                    connectorJson.accumulate("lastSync", getLastSync(user.getId(), conn));
                    connectorJson.accumulate("latestData", getLatestData(user.getId(), conn));
                    connectorJson.accumulate("errors", checkForErrors(user.getId(), conn));
                    connectorJson.accumulate("syncing", checkIfSyncInProgress(user.getId(), conn));
                    connectorJson.accumulate("channels", settingsService.getChannelsForConnector(user.getId(),conn));
                    connectorsArray.add(connectorJson);
                }
            }
            return connectorsArray.toString();
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get installed connectors: " + e.getMessage()));
        }
    }

    @GET
    @Path("/uninstalled")
    @Produces({MediaType.APPLICATION_JSON})
    public String getUninstalledConnectors(){
        try{
            Guest user = ControllerHelper.getGuest();
            List<ConnectorInfo> allConnectors =  sysService.getConnectors();
            List<ConnectorInfo> connectors = new ArrayList<ConnectorInfo>();
            for (ConnectorInfo connector : allConnectors) {
                if (connector.enabled)
                    connectors.add(connector);
            }
            for (int i = 0; i < connectors.size(); i++){
                if (guestService.hasApiKey(user.getId(), connectors.get(i).getApi()))
                    connectors.remove(i--);
            }

            return gson.toJson(connectors);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get uninstalled connectors: " + e.getMessage()));
        }
    }

    private boolean checkIfSyncInProgress(long guestId, Connector connector){
        final List<UpdateWorkerTask> scheduledUpdates = connectorUpdateService.getUpdatingUpdateTasks(guestId, connector);
        return (scheduledUpdates.size()!=0);
    }

    /**
     * Returns whether there was an error in the last update of the connector
     * @param guestId The id of the guest whose connector is being checked
     * @param connector the connector being checked
     * @return true if there was and error false otherwise
     */
    private boolean checkForErrors(long guestId, Connector connector){
        Collection<UpdateWorkerTask> update = connectorUpdateService.getLastFinishedUpdateTasks(guestId, connector);
        if(update.size() < 1) return false;
        for(UpdateWorkerTask workerTask : update)
        {
            if(workerTask == null || workerTask.status!= UpdateWorkerTask.Status.DONE) return true;
        }
        return false;
    }

    private long getLastSync(long guestId, Connector connector){
        ApiUpdate update = connectorUpdateService.getLastSuccessfulUpdate(guestId, connector);
        return update != null ? update.ts : Long.MAX_VALUE;

    }

    private long getLatestData(long guestId, Connector connector){
        AbstractFacet facet = apiDataService.getLatestApiDataFacet(guestId, connector, null);
        return facet == null ? Long.MAX_VALUE : facet.end;
    }

    @DELETE
    @Path("/{connector}")
    @Produces({MediaType.APPLICATION_JSON})
    public String deleteConnector(@PathParam("connector") String connector){
        StatusModel result;
        try{
            Guest user = ControllerHelper.getGuest();
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
    @Path("/{connector}/channels")
    @Produces({MediaType.APPLICATION_JSON})
    public String setConnectorChannels(@PathParam("connector") String connectorName, @FormParam("channels") String channels){
        StatusModel result;
        try{
            Guest user = ControllerHelper.getGuest();
            ApiKey apiKey = guestService.getApiKey(user.getId(), Connector.getConnector(connectorName));
            settingsService.setChannelsForConnector(user.getId(),apiKey.getConnector(),channels.split(","));
            result = new StatusModel(true,"Successfully updated channels for " + connectorName + ".");
        }
        catch (Exception e){
            result = new StatusModel(false,"Failed to set channels for " + connectorName + ".");
        }
        return gson.toJson(result);
    }

    @GET
    @Path("/filters")
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectorFilterState(){
        try{
            Guest user = ControllerHelper.getGuest();
            return settingsService.getConnectorFilterState(user.getId());
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get filters: " + e.getMessage()));
        }
    }

    @POST
    @Path("/filters")
    @Produces({MediaType.APPLICATION_JSON})
    public String setConnectorFilterState(@FormParam("filterState") String stateJSON){
        StatusModel result;
        try{
            Guest user = ControllerHelper.getGuest();
            settingsService.setConnectorFilterState(user.getId(), stateJSON);
            result = new StatusModel(true,"Successfully updated filters state!");
        }
        catch (Exception e){
            result = new StatusModel(false,"Failed to udpate filters state!");
        }
        return gson.toJson(result);
    }

}
