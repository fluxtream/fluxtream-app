package com.fluxtream.api;

import java.io.IOException;
import java.util.List;
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
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.ScheduleResult;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiKeyAttribute;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.ConnectorInfo;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.SystemService;
import com.fluxtream.services.WidgetsService;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.codehaus.plexus.util.ExceptionUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

@Path("/admin")
@Component("RESTAdminController")
@Scope("request")
public class AdminController {

	@Autowired
	GuestService guestService;

	Gson gson = new Gson();

    @Autowired
    JPADaoService jpaDaoService;

	@Autowired
	Configuration env;

    @Autowired
    WidgetsService widgetsService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    SystemService sysService;

    @GET
    @Secured({ "ROLE_ADMIN" })
	@Path("/properties/{propertyName}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getProperty(@PathParam("propertyName") String propertyName)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {

		if (env.get(propertyName) != null) {
			JSONObject property = new JSONObject();
			property.accumulate("name", propertyName).accumulate("value",
					env.get(propertyName));
			return property.toString();
		}

		StatusModel failure = new StatusModel(false, "property not found: "
				+ propertyName);
		return gson.toJson(failure);
	}

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/{username}/metadata/rebuild")
    @Produces({ MediaType.APPLICATION_JSON })
    public String populateBetterMetadataTables(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {

        try {
            metadataService.rebuildMetadata(username);
            StatusModel success = new StatusModel(true, "done");
            return gson.toJson(success);
        } catch (Throwable t) {
            StatusModel failure = new StatusModel(false, ExceptionUtils.getStackTrace(t));
            return gson.toJson(failure);
        }
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/timebounds/fixup")
    @Produces({ MediaType.APPLICATION_JSON })
    public String fixUpTimeBounds()
            throws Exception {
        List<ConnectorInfo> connectors =  sysService.getConnectors();
        for (ConnectorInfo connectorInfo : connectors) {
            final Connector connector = connectorInfo.getApi();
            if (connector==null)
                continue;
            else {
                List<Guest> guests = guestService.getAllGuests();
                for (Guest g : guests) {
                    ApiKey apiKey = guestService.getApiKey(g.getId(), connector);
                    if (apiKey==null) continue;
                    final ObjectType[] objectTypes = connector.objectTypes();
                    if (objectTypes!=null&&objectTypes.length>0) {
                        for (ObjectType objectType : objectTypes)
                            saveTimeBoundaries(apiKey, objectType);
                    } else
                        saveTimeBoundaries(apiKey, null);
                    ApiUpdate update = connectorUpdateService.getLastSuccessfulUpdate(apiKey);
                    if (update!=null) {
                        guestService.setApiKeyAttribute(apiKey,
                                                        ApiKeyAttribute.LAST_SYNC_TIME_KEY,
                                                        ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().print(update.ts));
                    }
                }
            }
        }
        StatusModel success = new StatusModel(true, "done");
        return gson.toJson(success);
    }

    private void saveTimeBoundaries(final ApiKey apiKey, final ObjectType objectType) {
        final AbstractFacet oldestApiDataFacet = apiDataService.getOldestApiDataFacet(apiKey, objectType);
        if (oldestApiDataFacet!=null)
            guestService.setApiKeyAttribute(apiKey,
                                            objectType==null
                                                       ? ApiKeyAttribute.MIN_TIME_KEY
                                                       : objectType.getApiKeyAttributeName(ApiKeyAttribute.MIN_TIME_KEY),
                                            ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().print(oldestApiDataFacet.start));
        final AbstractFacet latestApiDataFacet = apiDataService.getLatestApiDataFacet(apiKey, objectType);
        if (latestApiDataFacet!=null)
            guestService.setApiKeyAttribute(apiKey,
                                            objectType==null
                                                       ? ApiKeyAttribute.MAX_TIME_KEY
                                                       : objectType.getApiKeyAttributeName(ApiKeyAttribute.MAX_TIME_KEY),
                                            ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().print(Math.max(latestApiDataFacet.end, latestApiDataFacet.start)));
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/cleanup")
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteStaleData()
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {

        try {
            apiDataService.deleteStaleData();
            StatusModel success = new StatusModel(true, "done");
            return gson.toJson(success);
        } catch (Throwable t) {
            StatusModel failure = new StatusModel(false, ExceptionUtils.getStackTrace(t));
            return gson.toJson(failure);
        }
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/executeUpdate")
    @Produces({ MediaType.APPLICATION_JSON })
    public String executeUpdate(@FormParam("jpql") String jpql)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try {
            int results = jpaDaoService.execute(jpql);
            StatusModel result = new StatusModel(true, results + " rows affected");
            return gson.toJson(result);
        } catch (Exception e) {
            StatusModel failure = new StatusModel(false, "Could not execute query: " + e.getMessage());
            return gson.toJson(failure);
        }

    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/widgets/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public String refreshWidgets() {
        widgetsService.refreshWidgets();
        return gson.toJson(new StatusModel(true, "widgets refreshed"));
    }

    @GET
    @Secured({ "ROLE_ADMIN" })
    @Path("/privileges")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> listRoles() throws IOException {
        final Guest guest = AuthHelper.getGuest();
        final List<String> userRoles = guest.getUserRoles();
        return userRoles;
    }

    @GET
    @Path("/guests")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public String getGuestIds() throws IOException {
        final List<Guest> allGuests = guestService.getAllGuests();
        JSONArray jsonArray = new JSONArray();
        for (Guest guest : allGuests) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", guest.username);
            jsonObject.put("id", guest.getId());
            jsonArray.add(jsonObject);
        }
        return jsonArray.toString();
    }

    @GET
    @Path("/{username}/{connectorName}/apiKeys")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public List<ApiKey> listApiKeys(@PathParam("username") String username,
                                    @PathParam("connectorName") String connectorName) throws IOException {
        final Guest guest = guestService.getGuest(username);
        final List<ApiKey> apiKeys = guestService.getApiKeys(guest.getId(), Connector.getConnector(connectorName));
        return apiKeys;
    }

    @DELETE
    @Path("/apiKeys/{apiKeyId}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel deleteApiKey(@PathParam("apiKeyId") long apiKeyId) throws IOException {
        guestService.removeApiKey(apiKeyId);
        return new StatusModel(true, "apiKey was deleted");
    }

    @POST
    @Path("/apiKeys/{apiKeyId}/attribute")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel setApiKeyAttributeValue(@PathParam("apiKeyId") long apiKeyId,
                                               @FormParam("attributeKey") String attributeKey,
                                               @FormParam("attributeValue") String attributeValue) throws IOException {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        guestService.setApiKeyAttribute(apiKey, attributeKey, attributeValue);
        return new StatusModel(true, "attribute value was set");
    }

    @POST
    @Path("/apiKeys/{apiKeyId}/attribute/add")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel addApiKeyAttribute(@PathParam("apiKeyId") long apiKeyId,
                                          @FormParam("attributeKey") String attributeKey,
                                          @FormParam("attributeValue") String attributeValue) throws IOException {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final String existingValue = guestService.getApiKeyAttribute(apiKey, attributeKey);
        if (existingValue!=null)
            return new StatusModel(false, "This attribute already exists. Please edit the value if you want to change it.");
        guestService.setApiKeyAttribute(apiKey, attributeKey, attributeValue);
        return new StatusModel(true, "attribute was created");
    }

    @DELETE
    @Path("/apiKeys/{apiKeyId}/{attributeKey}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel deleteApiKeyAttribute(@PathParam("apiKeyId") long apiKeyId,
                                        @PathParam("attributeKey") String attributeKey) throws IOException {
        guestService.removeApiKeyAttribute(apiKeyId, attributeKey);
        return new StatusModel(true, "attribute was deleted");
    }

    @DELETE
    @Path("/guests/{guestId}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.TEXT_PLAIN})
    public String deleteUser(@PathParam("guestId") long guestId) throws Exception {
        final Guest guest = guestService.getGuestById(guestId);
        guestService.eraseGuestInfo(guestId);
        return "Deleted guest: " + guest.username;
    }

    @POST
    @Path("/reset/{username}/{connector}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel resetConnector(@PathParam("username") String username,
                                      @PathParam("connector") String connectorName) {
        final long guestId = guestService.getGuest(username).getId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);
        return new StatusModel(true, "reset controller " + connectorName);
    }

    @POST
    @Path("/{username}/password")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel setPassword(@PathParam("username") String username,
                                   @QueryParam("password") String password){
        final long guestId = guestService.getGuest(username).getId();
        guestService.setPassword(guestId, password);
        return new StatusModel(true, "set password for user " + username);
    }

    @POST
    @Path("/sync/{username}/{connector}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public String updateConnector(@PathParam("username") String username,
                                  @PathParam("connector") String connectorName){
        final long guestId = guestService.getGuest(username).getId();
        return sync(guestId, connectorName, true);
    }

    private String sync(final long guestId, final String connectorName, final boolean force) {
        try{
            final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnector(apiKey, force);
            StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
            statusModel.payload = scheduleResults;
            return gson.toJson(scheduleResults);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to schedule update: " + e.getMessage()));
        }
    }

    @GET
    @Path("/ping")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.TEXT_PLAIN})
    public String ping() throws IOException {
        final Guest guest = AuthHelper.getGuest();
        return "pong, " + guest.username;
    }

}
