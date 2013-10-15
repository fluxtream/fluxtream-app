package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.api.gson.UpdateInfoSerializer;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateFailedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
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
import org.springframework.beans.factory.BeanFactory;
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

    FlxLogger logger = FlxLogger.getLogger(ConnectorStore.class);

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

    @Autowired
    Configuration env;

    @Autowired
    BeanFactory beanFactory;

    Gson gson;

    public ConnectorStore() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UpdateInfo.class, new UpdateInfoSerializer());
        gson = gsonBuilder.create();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/settings/reset/{apiKeyId}")
    public String resetConnectorSettings(@PathParam("apiKeyId") long apiKeyId) {
        settingsService.resetConnectorSettings(apiKeyId);
        StatusModel status = new StatusModel(true, "connector settings reset!");
        return gson.toJson(status);
    }

    @GET
    @Path("/settings/{apiKeyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectorSettings(@PathParam("apiKeyId") long apiKeyId) throws UpdateFailedException {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final long guestId = AuthHelper.getGuestId();
        if (apiKey.getGuestId()!=guestId)
            throw new RuntimeException("attempt to retrieve ApiKey from another guest!");
        final Object settings = settingsService.getConnectorSettings(apiKey.getId(), true);
        String json = gson.toJson(settings);
        return json;
    }

    @POST
    @Path("/settings/{apiKeyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel saveConnectorSettings(@PathParam("apiKeyId") long apiKeyId,
                                             @FormParam("json") String json) {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final long guestId = AuthHelper.getGuestId();
        try {
            if (apiKey.getGuestId()!=guestId)
                throw new RuntimeException("attempt to retrieve ApiKey from another guest!");
            settingsService.saveConnectorSettings(apiKey.getId(), json);
        } catch (Throwable e) {
            return new StatusModel(false, e.getMessage());
        }
        return new StatusModel(true, "saved connector settings");
    }

    @POST
    @Path("/renew/{apiKeyId}")
    @Produces({MediaType.APPLICATION_JSON})
    public String renewConnectorTokens(@PathParam("apiKeyId") long apiKeyId) throws Exception {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        ConnectorInfo connectorInfo = sysService.getConnectorInfo(apiKey.getConnector().getName());
        JSONObject renewInfo = new JSONObject();
        final String renewTokensUrlTemplate = connectorInfo.renewTokensUrlTemplate;
        final String renewTokensUrl = String.format(renewTokensUrlTemplate, apiKey.getId());
        renewInfo.accumulate("redirectTo", env.get("homeBaseUrl") + renewTokensUrl);
        return renewInfo.toString();
    }

    @GET
    @Path("/installed")
    @Produces({MediaType.APPLICATION_JSON})
    public String getInstalledConnectors(){
        Guest guest = AuthHelper.getGuest();
        // If no guest is logged in, return empty array
        if(guest==null)
            return "[]";
        ResourceBundle res = ResourceBundle.getBundle("messages/connectors");
        try {
            List<ConnectorInfo> connectors =  sysService.getConnectors();
            JSONArray connectorsArray = new JSONArray();
            for (int i = 0; i < connectors.size(); i++) {
                final ConnectorInfo connectorInfo = connectors.get(i);
                final Connector api = connectorInfo.getApi();
                if (api==null) {
                    StringBuilder sb = new StringBuilder("module=API component=connectorStore action=getInstalledConnectors ");
                    logger.warn("message=\"null connector for " + connectorInfo.getName() + "\"");
                    continue;
                }
                if (!guestService.hasApiKey(guest.getId(), api)||api.getName().equals("facebook")/*HACK*/) {
                    connectors.remove(i--);
                }
                else {
                    ConnectorInfo connector = connectorInfo;
                    JSONObject connectorJson = new JSONObject();
                    Connector conn = Connector.fromValue(connector.api);
                    ApiKey apiKey = guestService.getApiKey(guest.getId(), conn);

                    connectorJson.accumulate("prettyName", conn.prettyName());
                    List<String> facetTypes = new ArrayList<String>();
                    ObjectType[] objTypes = conn.objectTypes();
                    if (objTypes != null) {
                        for (ObjectType obj : objTypes) {
                            facetTypes.add(connector.connectorName + "-" + obj.getName());
                        }
                    }
                    connectorJson.accumulate("facetTypes", facetTypes);
                    connectorJson.accumulate("status", apiKey.status!=null?apiKey.status.toString():"NA");
                    connectorJson.accumulate("name", connector.name);
                    connectorJson.accumulate("connectUrl", connector.connectUrl);
                    connectorJson.accumulate("image", connector.image);
                    connectorJson.accumulate("connectorName", connector.connectorName);
                    connectorJson.accumulate("enabled", connector.enabled);
                    connectorJson.accumulate("manageable", connector.manageable);
                    connectorJson.accumulate("text", connector.text);
                    connectorJson.accumulate("api", connector.api);
                    connectorJson.accumulate("apiKeyId", apiKey.getId());
                    connectorJson.accumulate("lastSync", getLastSync(apiKey));
                    connectorJson.accumulate("latestData", getLatestData(apiKey));
                    final String auditTrail = checkForErrors(apiKey);
                    connectorJson.accumulate("errors", auditTrail!=null);
                    connectorJson.accumulate("auditTrail", auditTrail!=null?auditTrail:"");
                    connectorJson.accumulate("syncing", checkIfSyncInProgress(guest.getId(), conn));
                    connectorJson.accumulate("channels", settingsService.getChannelsForConnector(guest.getId(), conn));
                    connectorJson.accumulate("sticky", connector.connectorName.equals("fluxtream_capture"));
                    connectorJson.accumulate("supportsRenewToken", connector.supportsRenewTokens);
                    connectorJson.accumulate("supportsSync", connector.supportsSync);
                    connectorJson.accumulate("supportsFileUpload", connector.supportsFileUpload);
                    connectorJson.accumulate("prettyName", conn.prettyName());
                    final String uploadMessageKey = conn.getName() + ".upload";
                    if (res.containsKey(uploadMessageKey)) {
                        final String uploadMessage = res.getString(uploadMessageKey);
                        connectorJson.accumulate("uploadMessage", uploadMessage);
                    }
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
        Guest guest = AuthHelper.getGuest();
        // If no guest is logged in, return empty array
        if(guest==null)
            return "[]";
        try {
            List<ConnectorInfo> allConnectors =  sysService.getConnectors();
            List<ConnectorInfo> connectors = new ArrayList<ConnectorInfo>();
            for (ConnectorInfo connector : allConnectors) {
                if (connector.enabled&&!connector.connectorName.equals("facebook"))
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
        final ApiKey apiKey = guestService.getApiKey(guestId, connector);
        return (apiKey.synching);
    }


    private String checkForErrors(ApiKey apiKey) {
        return apiKey.stackTrace;
    }

    private long getLastSync(ApiKey apiKey) {
        ApiUpdate update = connectorUpdateService.getLastSuccessfulUpdate(apiKey);
        return update != null ? update.ts : Long.MAX_VALUE;
    }

    private long getLatestData(ApiKey apiKey) {
        AbstractFacet facet = apiDataService.getLatestApiDataFacet(apiKey, null);
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
            guestService.removeApiKeys(guest.getId(), apiToRemove);
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

    @GET
    @Path("/{objectTypeName}/data")
    @Produces({MediaType.APPLICATION_JSON})
    public String getData(@PathParam("objectTypeName") String objectTypeName, @QueryParam("start") long start, @QueryParam("end") long end, @QueryParam("value") String value){
        Guest guest = AuthHelper.getGuest();
        if(guest==null)
            return "[]";
        String [] objectTypeNameParts = objectTypeName.split("-");
        ApiKey apiKey = guestService.getApiKeys(guest.getId(),Connector.getConnector(objectTypeNameParts[0])).get(0);
        Connector connector = apiKey.getConnector();

        return gson.toJson(connector.getBodytrackResponder(beanFactory).getFacetVOs(settingsService.getSettings(guest.getId()), apiKey,objectTypeName,start,end,value));

    }
}
