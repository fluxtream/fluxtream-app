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
import com.fluxtream.api.gson.UpdateInfoSerializer;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.UpdateWorkerTask;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

/**
 *
 */
@Path("/connectors")
@Component("RESTConnectorStore")
@Scope("request")
public class ConnectorStore {

    Logger logger = Logger.getLogger(ConnectorStore.class);

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

    Gson gson;

    public ConnectorStore() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UpdateInfo.class, new UpdateInfoSerializer());
        gson = gsonBuilder.create();
    }

    @GET
    @Path("/installed")
    @Produces({MediaType.APPLICATION_JSON})
    public String getInstalledConnectors(){
        setTransactionName(null, "GET /connectors/installed");
        Guest guest = AuthHelper.getGuest();
        // If no guest is logged in, return empty array
        if(guest==null)
            return "[]";
        try {
            List<ConnectorInfo> connectors =  sysService.getConnectors();
            JSONArray connectorsArray = new JSONArray();
            for (int i = 0; i < connectors.size(); i++){
                if (!guestService.hasApiKey(guest.getId(), connectors.get(i).getApi())) {
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
                    connectorJson.accumulate("lastSync", getLastSync(guest.getId(), conn));
                    connectorJson.accumulate("latestData", getLatestData(guest.getId(), conn));
                    connectorJson.accumulate("errors", checkForErrors(guest.getId(), conn));
                    connectorJson.accumulate("syncing", checkIfSyncInProgress(guest.getId(), conn));
                    connectorJson.accumulate("channels", settingsService.getChannelsForConnector(guest.getId(), conn));
                    connectorsArray.add(connectorJson);
                }
            }
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getInstalledConnectors")
                    .append(" guestId=").append(guest.getId());
            logger.info(sb.toString());
            return connectorsArray.toString();
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getInstalledConnectors")
                    .append(" guestId=").append(guest.getId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get installed connectors: " + e.getMessage()));
        }
    }

    @GET
    @Path("/uninstalled")
    @Produces({MediaType.APPLICATION_JSON})
    public String getUninstalledConnectors(){
        setTransactionName(null, "GET /connectors/uninstalled");
        Guest guest = AuthHelper.getGuest();
        // If no guest is logged in, return empty array
        if(guest==null)
            return "[]";
        try {
            List<ConnectorInfo> allConnectors =  sysService.getConnectors();
            List<ConnectorInfo> connectors = new ArrayList<ConnectorInfo>();
            for (ConnectorInfo connector : allConnectors) {
                if (connector.enabled)
                    connectors.add(connector);
            }
            for (int i = 0; i < connectors.size(); i++){
                if (guestService.hasApiKey(guest.getId(), connectors.get(i).getApi()))
                    connectors.remove(i--);
            }
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getUninstalledConnectors")
                    .append(" guestId=").append(guest.getId());
            logger.info(sb.toString());
            return gson.toJson(connectors);
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getUninstalledConnectors")
                    .append(" guestId=").append(guest.getId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get uninstalled connectors: " + e.getMessage()));
        }
    }

    private boolean checkIfSyncInProgress(long guestId, Connector connector){
        final Collection<UpdateWorkerTask> scheduledUpdates = connectorUpdateService.getUpdatingUpdateTasks(guestId, connector);
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
        Guest guest = AuthHelper.getGuest();
        // If no guest is logged in, return empty array
        if(guest==null)
            return "{}";
        try{
            Connector apiToRemove = Connector.fromString(connector);
            guestService.removeApiKey(guest.getId(), apiToRemove);
            result = new StatusModel(true,"Successfully removed " + connector + ".");
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=deleteConnector")
                    .append(" connector=").append(connector)
                    .append(" guestId=").append(guest.getId());
            logger.info(sb.toString());
        }
        catch (Exception e){
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=deleteConnector")
                    .append(" connector=").append(connector)
                    .append(" guestId=").append(guest.getId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            result = new StatusModel(false,"Failed to remove " + connector + ".");
        }
        return gson.toJson(result);
    }

    @POST
    @Path("/{connector}/channels")
    @Produces({MediaType.APPLICATION_JSON})
    public String setConnectorChannels(@PathParam("connector") String connectorName, @FormParam("channels") String channels){
        StatusModel result;
        Guest guest = AuthHelper.getGuest();
        // If no guest is logged in, return empty array
        if(guest==null)
            return "{}";
        try {
            ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector(connectorName));
            settingsService.setChannelsForConnector(guest.getId(),apiKey.getConnector(),channels.split(","));
            result = new StatusModel(true,"Successfully updated channels for " + connectorName + ".");
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=setConnectorChannels")
                    .append(" connector=").append(connectorName)
                    .append(" channels=").append(channels)
                    .append(" guestId=").append(guest.getId());
            logger.info(sb.toString());
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=setConnectorChannels")
                    .append(" connector=").append(connectorName)
                    .append(" guestId=").append(guest.getId())
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            result = new StatusModel(false,"Failed to set channels for " + connectorName + ".");
        }
        return gson.toJson(result);
    }

    @GET
    @Path("/filters")
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectorFilterState(){
        long vieweeId = AuthHelper.getGuestId();
        try {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getConnectorFilterState")
                    .append(" guestId=").append(vieweeId);
            logger.info(sb.toString());
            return settingsService.getConnectorFilterState(vieweeId);
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getConnectorFilterState")
                    .append(" guestId=").append(vieweeId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get filters: " + e.getMessage()));
        }
    }

    @POST
    @Path("/filters")
    @Produces({MediaType.APPLICATION_JSON})
    public String setConnectorFilterState(@FormParam("filterState") String stateJSON){
        StatusModel result;
        Guest guest = AuthHelper.getGuest();
        if(guest==null)
            return "{}";
        try {
            settingsService.setConnectorFilterState(guest.getId(), stateJSON);
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=setConnectorFilterState")
                    .append(" filterState=").append(stateJSON)
                    .append(" guestId=").append(guest.getId());
            logger.info(sb.toString());
            result = new StatusModel(true,"Successfully updated filters state!");
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=API component=connectorStore action=setConnectorFilterState")
                    .append(" guestId=").append(guest.getId())
                    .append(" filterState=").append(stateJSON)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
            result = new StatusModel(false,"Failed to udpate filters state!");
        }
        return gson.toJson(result);
    }

}
