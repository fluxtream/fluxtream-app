package org.fluxtream.core.api;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.ScheduleResult;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ApiKeyAttribute;
import org.fluxtream.core.domain.ApiUpdate;
import org.fluxtream.core.domain.ConnectorInfo;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.services.WidgetsService;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
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
    @Path("/apiKeys/{apiKeyId}/attribute/delete")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public StatusModel deleteApiKeyAttribute(@PathParam("apiKeyId") long apiKeyId,
                                             @QueryParam("attributeKey") String attributeKey) throws IOException {
        attributeKey = URLDecoder.decode(attributeKey, "UTF-8");
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

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/sync/{username}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String sync(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        final long guestId = guestService.getGuest(username).getId();
        try {
            final List<ScheduleResult> scheduleResults = connectorUpdateService.updateAllConnectors(guestId, false);
            StatusModel statusModel = new StatusModel(true, "successfully added update worker tasks to the queue (see details)");
            statusModel.payload = scheduleResults;
            return gson.toJson(scheduleResults);
        } catch (Throwable t) {
            StatusModel failure = new StatusModel(false, ExceptionUtils.getStackTrace(t));
            return gson.toJson(failure);
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

    @GET
    @Path("/{connector}/oauthTokens")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getOAuthTokens(@PathParam("connector") String connectorName)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try{
            long guestId = AuthHelper.getGuestId();

            ApiKey apiKey = guestService.getApiKey(guestId,
                                                   Connector.getConnector(connectorName));

            if (apiKey != null) {
                final Map<String,String> atts = apiKey.getAttributes(env);

                return gson.toJson(atts);
            } else {
                StatusModel result = new StatusModel(false,
                                                     "Guest does not have that connector: " + connectorName);
                return gson.toJson(result);
            }
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get OAuth Tokens: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{connector}/oauthTokens")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String setOAuthTokens(@PathParam("connector") String connectorName,
                                 @FormParam("accessToken")String accessToken,
                                 @FormParam("tokenSecret")String tokenSecret)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try{
            long guestId = AuthHelper.getGuestId();

            ApiKey apiKey = guestService.getApiKey(guestId,
                                                   Connector.getConnector(connectorName));
            if (apiKey != null) {
                guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken);
                guestService.setApiKeyAttribute(apiKey, "tokenSecret", tokenSecret);

                StatusModel result = new StatusModel(true,
                                                     "Successfully updated oauth tokens: " + connectorName);
                return gson.toJson(result);
            } else {
                StatusModel result = new StatusModel(false,
                                                     "Guest does not have that connector: " + connectorName);
                return gson.toJson(result);
            }
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to set OAuth Tokens: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{connector}/oauth2Tokens")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String setOAuth2Tokens(@PathParam("connector") String connectorName,
                                  @FormParam("tokenExpires") String tokenExpires,
                                  @FormParam("refreshTokenRemoveURL") String refreshTokenRemoveURL,
                                  @FormParam("accessToken") String accessToken,
                                  @FormParam("refreshToken")String refreshToken)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try{
            long guestId = AuthHelper.getGuestId();

            ApiKey apiKey = guestService.getApiKey(guestId,
                                                   Connector.getConnector(connectorName));
            if (apiKey != null) {

                guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken);
                guestService.setApiKeyAttribute(apiKey, "tokenExpires", tokenExpires);
                guestService.setApiKeyAttribute(apiKey, "refreshTokenRemoveURL", refreshTokenRemoveURL);
                guestService.setApiKeyAttribute(apiKey, "refreshToken", refreshToken);

                StatusModel result = new StatusModel(true,
                                                     "Successfully updated oauth2 tokens: " + connectorName);
                return gson.toJson(result);
            } else {
                StatusModel result = new StatusModel(false,
                                                     "Guest does not have that connector: " + connectorName);
                return gson.toJson(result);
            }
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to set OAuth2 Tokens: " + e.getMessage()));
        }
    }

    @POST
    @Path("/create")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createGuest(@FormParam("username") String username,
                              @FormParam("firstname") String firstname,
                              @FormParam("lastname") String lastname,
                              @FormParam("password") String password,
                              @FormParam("email") String email) throws InstantiationException, IllegalAccessException,
                                                                       ClassNotFoundException, UsernameAlreadyTakenException, ExistingEmailException {
        try {
            guestService.createGuest(username, firstname, lastname, password,
                                     email, Guest.RegistrationMethod.REGISTRATION_METHOD_FORM, false);
            StatusModel result = new StatusModel(true, "User " + username
                                                       + " was successfully created");
            return gson.toJson(result);
        } catch (Exception e) {
            StatusModel result = new StatusModel(false,
                                                 "Could not create guest: " + e.getMessage());
            return gson.toJson(result);
        }

    }

    @DELETE
    @Secured({ "ROLE_ADMIN" })
    @Path("/{username}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteGuest(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try {
            guestService.eraseGuestInfo(username);
            StatusModel result = new StatusModel(true, "User " + username
                                                       + " was successfully deleted");
            return gson.toJson(result);
        } catch (Exception e) {
            StatusModel result = new StatusModel(false,
                                                 "Could not delete guest: " + e.getMessage());
            return gson.toJson(result);
        }

    }

    @GET
    @Path("/list")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String list() throws InstantiationException, IllegalAccessException,
                                ClassNotFoundException {
        try{
            final Guest me = AuthHelper.getGuest();

            List<Guest> list = guestService.getAllGuests();
            JSONArray array = new JSONArray();
            for (Guest guest : list) {
                JSONObject guestJson = new JSONObject();
                guestJson.accumulate("id", guest.getId())
                        .accumulate("username", guest.username)
                        .accumulate("firstname", guest.firstname)
                        .accumulate("lastname", guest.lastname)
                        .accumulate("email", guest.email)
                        .accumulate("roles", guest.getUserRoles());
                array.add(guestJson);
            }
            return array.toString();
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to list guests: " + e.getMessage()));
        }
    }

    @GET
    @Path("/{username}/roles")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getRoles(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        try {
            Guest guest = guestService.getGuest(username);
            JSONArray array = getGuestRolesJsonArray(guest);
            return array.toString();
        } catch (Exception e) {
            StatusModel result = new StatusModel(false,
                                                 "Could not get roles: " + e.getMessage());
            return gson.toJson(result);
        }

    }

    @POST
    @Path("/{username}/roles")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public String setRoles(@PathParam("username") String username,
                           @FormParam("roles") String roles) throws InstantiationException,
                                                                    IllegalAccessException, ClassNotFoundException {
        try {
            Guest guest = guestService.getGuest(username);
            StringTokenizer st = new StringTokenizer(roles, ",");
            List<String> addedRoles = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                String newRole = st.nextToken();
                List<String> userRoles = guest.getUserRoles();
                for (String existingRole : userRoles) {
                    if (existingRole.toLowerCase()
                            .equals(newRole.toLowerCase()))
                        continue;
                }
                guestService.addRole(guest.getId(), newRole);
            }

            guest = guestService.getGuest(username);
            JSONArray array = getGuestRolesJsonArray(guest);
            JSONObject result = new JSONObject();
            result.accumulate("result", "OK")
                    .accumulate(
                            "message",
                            "successfully added role "
                            + StringUtils.join(addedRoles, ", "))
                    .accumulate("user_roles:", array);
            return result.toString();
        } catch (Exception e) {
            StatusModel result = new StatusModel(false,
                                                 "Could not grant role: " + e.getMessage());
            return gson.toJson(result);
        }

    }

    @DELETE
    @Secured({ "ROLE_ADMIN" })
    @Path("/{username}/roles/{role}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String revokeRole(@PathParam("username") String username,
                             @PathParam("role") String role) throws InstantiationException,
                                                                    IllegalAccessException, ClassNotFoundException {
        try {
            Guest guest = guestService.getGuest(username);
            guestService.removeRole(guest.getId(), role);

            guest = guestService.getGuest(username);
            JSONArray array = getGuestRolesJsonArray(guest);
            JSONObject result = new JSONObject();
            result.accumulate("result", "OK")
                    .accumulate("message", "successfully removed role " + role)
                    .accumulate("user_roles:", array);
            return result.toString();
        } catch (Exception e) {
            StatusModel result = new StatusModel(false,
                                                 "Could not revoke role: " + e.getMessage());
            return gson.toJson(result);
        }

    }

    private JSONArray getGuestRolesJsonArray(Guest guest) {
        JSONArray array = new JSONArray();
        List<String> userRoles = guest.getUserRoles();
        for (String userRole : userRoles)
            array.add(userRole);
        return array;
    }

}
