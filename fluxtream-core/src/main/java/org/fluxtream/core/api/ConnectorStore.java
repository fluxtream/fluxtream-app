package org.fluxtream.core.api;

import java.io.IOException;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.api.gson.UpdateInfoSerializer;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.CoachRevokedException;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiKeyAttribute;
import org.fluxtream.core.domain.ApiUpdate;
import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.ConnectorInfo;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SettingsService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 */
@Path("/connectors")
@Component("RESTConnectorStore")
@Api(value = "/connectors", description = "Connector and connector settings management operations (list, add, remove, etc.)")
@Scope("request")
public class ConnectorStore {

    FlxLogger logger = FlxLogger.getLogger(ConnectorStore.class);

    @Autowired
    GuestService guestService;

    @Autowired
    SystemService sysService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    private ApiDataService apiDataService;

    @Autowired
    Configuration env;

    @Autowired
    BeanFactory beanFactory;

    Gson gson;
    ObjectMapper mapper = new ObjectMapper();

    public ConnectorStore() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UpdateInfo.class, new UpdateInfoSerializer());
        gson = gsonBuilder.create();
        mapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Reset connector settings to their default values", response = StatusModel.class,
                  notes="A set of default values are stored alongside user modified values for all connector settings",
                  authorizations = {@Authorization(value="oauth2")})
    @Path("/settings/reset/{apiKeyId}")
    public String resetConnectorSettings(@ApiParam(value="The connector's ApiKey ID", required=true) @PathParam("apiKeyId") long apiKeyId) {
        settingsService.resetConnectorSettings(apiKeyId);
        StatusModel status = new StatusModel(true, "connector settings reset!");
        return gson.toJson(status);
    }

    @GET
    @Path("/settings/{apiKeyId}")
    @ApiOperation(value = "Retrieve connector settings", response = StatusModel.class,
                  notes = "The structure of the returned object is connector dependent",
                  authorizations = {@Authorization(value="oauth2")})
    @Produces({MediaType.APPLICATION_JSON})
    public String getConnectorSettings(@ApiParam(value="The connector's ApiKey ID", required=true)  @PathParam("apiKeyId") long apiKeyId) throws UpdateFailedException, IOException {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final long guestId = AuthHelper.getGuestId();
        if (apiKey.getGuestId()!=guestId)
            throw new RuntimeException("attempt to retrieve ApiKey from another guest!");
        final Object settings = settingsService.getConnectorSettings(apiKey.getId());
        String json = mapper.writeValueAsString(settings);
        return json;
    }

    @POST
    @Path("/settings/{apiKeyId}")
    @ApiOperation(value = "Save user-modified connector settings", response = StatusModel.class,
                  notes = "The structure of the returned object is connector dependent",
                  authorizations = {@Authorization(value="oauth2")})
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel saveConnectorSettings(@ApiParam(value="The connector's ApiKey ID", required=true)  @PathParam("apiKeyId") long apiKeyId,
                                             @ApiParam(value="JSON-serialized connector settings object", required=true)  @FormParam("json") String json) {
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
    @ApiOperation(value = "Retrieve the list of installed (/added) connectors for the current user",
                  responseContainer = "Array", response = ConnectorInfo.class,
                  notes = "WARNING: there is more in the ConnectorInfo 'class' than what's specified here)",
                  authorizations = {@Authorization(value="oauth2")})
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
                    connectorJson.accumulate("lastSync", connector.supportsSync?getLastSync(apiKey):Long.MAX_VALUE);
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
            System.out.println(sb.toString());
            logger.warn(sb.toString());
            return gson.toJson(new StatusModel(false,"Failed to get installed connectors: " + e.getMessage()));
        }
    }

    @GET
    @Path("/uninstalled")
    @ApiOperation(value = "Retrieve the list of available (/not-yet-added) connectors for the current user",
                  responseContainer="Array", response = ConnectorInfo.class,
                  notes = "The structure of the returned object is connector dependent",
                  authorizations = {@Authorization(value="oauth2")})
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
        if (!apiKey.getConnector().hasFacets())
            return Long.MAX_VALUE;
        final String lastSyncTimeAtt = guestService.getApiKeyAttribute(apiKey, ApiKeyAttribute.LAST_SYNC_TIME_KEY);
        // only return the ApiKey's lastSyncTime if we have it cached as an attribute
        if (lastSyncTimeAtt !=null && StringUtils.isNotEmpty(lastSyncTimeAtt)) {
            final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(lastSyncTimeAtt);
            return dateTime.getMillis();
        }
        // fall back to old method of querying the ApiUpdates table
        ApiUpdate update = null;  // connectorUpdateService.getLastSuccessfulUpdate(apiKey);
        return update != null ? update.ts : Long.MAX_VALUE;
    }

    private long getLatestData(ApiKey apiKey) {
        if (!apiKey.getConnector().hasFacets())
            return Long.MAX_VALUE;
        final ObjectType[] objectTypes = apiKey.getConnector().objectTypes();
        if (objectTypes==null||objectTypes.length==0) {
            final String maxTimeAtt = guestService.getApiKeyAttribute(apiKey, ApiKeyAttribute.MAX_TIME_KEY);
            // only return the ApiKey's maxTime if we have it cached as an attribute
            if (maxTimeAtt !=null && StringUtils.isNotEmpty(maxTimeAtt)) {
                final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(maxTimeAtt);
                return dateTime.getMillis();
            }
        } else {
            long maxTime = Long.MIN_VALUE;
            for (ObjectType objectType : objectTypes) {
                final String maxTimeAtt = guestService.getApiKeyAttribute(apiKey, objectType.getApiKeyAttributeName(ApiKeyAttribute.MAX_TIME_KEY));
                if (maxTimeAtt !=null && StringUtils.isNotEmpty(maxTimeAtt)) {
                    final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(maxTimeAtt);
                    final long maxObjectTypeTime = dateTime.getMillis();
                    if (maxObjectTypeTime>maxTime)
                        maxTime = maxObjectTypeTime;
                }
            }
            // only return the ApiKey's maxTime if we have it cached as an attribute for any its connector's objectTypes
            if (maxTime>Long.MIN_VALUE)
                return maxTime;
        }
        // fall back to old method of querying the facets table
        AbstractFacet facet = null; // apiDataService.getLatestApiDataFacet(apiKey, null);
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

        CoachingBuddy coachee;
        try {
            coachee = AuthHelper.getCoachee();
        } catch (CoachRevokedException e) {
            return gson.toJson(new StatusModel(false, "Sorry, permission to access this data has been revoked. Please reload your browser window"));
        }
        if (coachee!=null) {
            guest = guestService.getGuestById(coachee.guestId);
        }

        String [] objectTypeNameParts = objectTypeName.split("-");
        ApiKey apiKey = guestService.getApiKeys(guest.getId(),Connector.getConnector(objectTypeNameParts[0])).get(0);
        Connector connector = apiKey.getConnector();

        final AbstractBodytrackResponder bodytrackResponder = connector.getBodytrackResponder(beanFactory);
        return gson.toJson(bodytrackResponder.getFacetVOs(settingsService.getSettings(guest.getId()), apiKey, objectTypeName, start, end, value));

    }
}
