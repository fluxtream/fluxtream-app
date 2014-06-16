package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.sun.jersey.api.Responses;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.updaters.ScheduleResult;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.services.*;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

@Path("/v1/admin")
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
            throws Exception {

		if (env.get(propertyName) != null) {
			JSONObject property = new JSONObject();
			property.accumulate("name", propertyName).accumulate("value",
					env.get(propertyName));
			return property.toString();
		}

        throw new Exception("property not found: " + propertyName);
	}

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/{username}/metadata/rebuild")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response populateBetterMetadataTables(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        metadataService.rebuildMetadata(username);
        return Response.noContent().build();
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/timebounds/fixup")
    public Response fixUpTimeBounds()
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
        return Response.status(202).build();
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
    public Response deleteStaleData()
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        apiDataService.deleteStaleData();
        return Response.status(202).build();
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/up/expireTokens")
    public Response expireJawboneUPAccessTokens()
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        final List<ApiKey> upKeys = jpaDaoService.findWithQuery("SELECT apiKey FROM ApiKey apiKey WHERE apiKey.api=?", ApiKey.class, Connector.getConnector("up").value());
        int i=0;
        for (ApiKey upKey : upKeys) {
            i++;
            guestService.setApiKeyAttribute(upKey, "tokenExpires", String.valueOf(System.currentTimeMillis()));
        }
        return Response.ok(i + " (up) tokens have been expired.").build();
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/executeUpdate")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeUpdate(@FormParam("jpql") String jpql)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        int results = jpaDaoService.execute(jpql);
        return Response.ok(results + " rows affected").build();
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/widgets/refresh")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response refreshWidgets() {
        widgetsService.refreshWidgets();
        return Response.noContent().build();
    }

    @GET
    @Secured({ "ROLE_ADMIN" })
    @Path("/privileges")
    @Produces({MediaType.APPLICATION_JSON})
    public Response listRoles() throws IOException {
        final Guest guest = AuthHelper.getGuest();
        final List<String> userRoles = guest.getUserRoles();
        return Response.ok(userRoles).build();
    }

    @GET
    @Path("/guests")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response getGuestIds() throws IOException {
        final List<Guest> allGuests = guestService.getAllGuests();
        JSONArray jsonArray = new JSONArray();
        for (Guest guest : allGuests) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", guest.username);
            jsonObject.put("id", guest.getId());
            jsonArray.add(jsonObject);
        }
        return Response.ok(jsonArray.toString()).build();
    }

    @GET
    @Path("/{username}/{connectorName}/apiKeys")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response listApiKeys(@PathParam("username") String username,
                                    @PathParam("connectorName") String connectorName) throws IOException {
        final Guest guest = guestService.getGuest(username);
        final List<ApiKey> apiKeys = guestService.getApiKeys(guest.getId(), Connector.getConnector(connectorName));
        return Response.ok(apiKeys).build();
    }

    @DELETE
    @Path("/apiKeys/{apiKeyId}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteApiKey(@PathParam("apiKeyId") long apiKeyId) throws IOException {
        guestService.removeApiKey(apiKeyId);
        return Response.noContent().build();
    }

    @POST
    @Path("/apiKeys/{apiKeyId}/attribute")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response setApiKeyAttributeValue(@PathParam("apiKeyId") long apiKeyId,
                                               @FormParam("attributeKey") String attributeKey,
                                               @FormParam("attributeValue") String attributeValue) throws IOException {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        guestService.setApiKeyAttribute(apiKey, attributeKey, attributeValue);
        return Response.ok(apiKey).build();
    }

    @POST
    @Path("/apiKeys/{apiKeyId}/attribute/add")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response addApiKeyAttribute(@PathParam("apiKeyId") long apiKeyId,
                                          @FormParam("attributeKey") String attributeKey,
                                          @FormParam("attributeValue") String attributeValue) throws IOException {
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        final String existingValue = guestService.getApiKeyAttribute(apiKey, attributeKey);
        if (existingValue!=null)
            return Responses.conflict().entity("An attribute with that name already exists").build();
        guestService.setApiKeyAttribute(apiKey, attributeKey, attributeValue);
        return Response.ok(apiKey).build();
    }

    @DELETE
    @Path("/apiKeys/{apiKeyId}/attribute/delete")
    @Secured({ "ROLE_ADMIN" })
    public Response deleteApiKeyAttribute(@PathParam("apiKeyId") long apiKeyId,
                                             @QueryParam("attributeKey") String attributeKey) throws IOException {
        attributeKey = URLDecoder.decode(attributeKey, "UTF-8");
        guestService.removeApiKeyAttribute(apiKeyId, attributeKey);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/guests/{guestId}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.TEXT_PLAIN})
    public Response deleteUser(@PathParam("guestId") long guestId) throws Exception {
        guestService.eraseGuestInfo(guestId);
        return Response.noContent().build();
    }

    @POST
    @Path("/reset/{username}/{connector}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response resetConnector(@PathParam("username") String username,
                                   @PathParam("connector") String connectorName) {
        final long guestId = guestService.getGuest(username).getId();
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);
        return Response.noContent().build();
    }

    @POST
    @Path("/{username}/password")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response setPassword(@PathParam("username") String username,
                                @QueryParam("password") String password){
        final long guestId = guestService.getGuest(username).getId();
        guestService.setPassword(guestId, password);
        return Response.noContent().build();
    }

    @POST
    @Path("/sync/{username}/{connector}")
    @Secured({ "ROLE_ADMIN" })
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateConnector(@PathParam("username") String username,
                                  @PathParam("connector") String connectorName) {
        final long guestId = guestService.getGuest(username).getId();
        return sync(guestId, connectorName, true);
    }

    private Response sync(final long guestId, final String connectorName, final boolean force) {
        final ApiKey apiKey = guestService.getApiKey(guestId, Connector.getConnector(connectorName));
        final List<ScheduleResult> scheduleResults = connectorUpdateService.updateConnector(apiKey, force);
        return Response.ok(scheduleResults).build();
    }

    @POST
    @Secured({ "ROLE_ADMIN" })
    @Path("/sync/{username}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response sync(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        final long guestId = guestService.getGuest(username).getId();
        final List<ScheduleResult> scheduleResults = connectorUpdateService.updateAllConnectors(guestId, false);
        return Response.ok(scheduleResults).build();
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
    public Response getOAuthTokens(@PathParam("connector") String connectorName)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        long guestId = AuthHelper.getGuestId();

        ApiKey apiKey = guestService.getApiKey(guestId,
                                               Connector.getConnector(connectorName));

        if (apiKey != null) {
            final Map<String,String> atts = apiKey.getAttributes(env);
            return Response.ok(atts).build();
        } else {
            return Responses.conflict().entity("Guest does not have that connector: " + connectorName).build();
        }
    }

    @POST
    @Path("/{connector}/oauthTokens")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setOAuthTokens(@PathParam("connector") String connectorName,
                                 @FormParam("accessToken")String accessToken,
                                 @FormParam("tokenSecret")String tokenSecret)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        long guestId = AuthHelper.getGuestId();

        ApiKey apiKey = guestService.getApiKey(guestId,
                                               Connector.getConnector(connectorName));
        if (apiKey != null) {
            guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken);
            guestService.setApiKeyAttribute(apiKey, "tokenSecret", tokenSecret);

            return Response.noContent().build();
        } else
            return Responses.conflict().entity("Guest does not have that connector: " + connectorName).build();
    }

    @POST
    @Path("/{connector}/oauth2Tokens")
    @Secured({ "ROLE_ADMIN" })
    public Response setOAuth2Tokens(@PathParam("connector") String connectorName,
                                  @FormParam("tokenExpires") String tokenExpires,
                                  @FormParam("refreshTokenRemoveURL") String refreshTokenRemoveURL,
                                  @FormParam("accessToken") String accessToken,
                                  @FormParam("refreshToken")String refreshToken)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        long guestId = AuthHelper.getGuestId();

        ApiKey apiKey = guestService.getApiKey(guestId,
                                               Connector.getConnector(connectorName));
        if (apiKey != null) {

            guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken);
            guestService.setApiKeyAttribute(apiKey, "tokenExpires", tokenExpires);
            guestService.setApiKeyAttribute(apiKey, "refreshTokenRemoveURL", refreshTokenRemoveURL);
            guestService.setApiKeyAttribute(apiKey, "refreshToken", refreshToken);

            return Response.ok("Successfully updated oauth2 tokens: " + connectorName).build();
        } else {
            return Responses.conflict().entity("Guest does not have that connector: " + connectorName).build();
        }
    }

    @POST
    @Path("/create")
    @Secured({ "ROLE_ADMIN" })
    public Response createGuest(@FormParam("username") String username,
                              @FormParam("firstname") String firstname,
                              @FormParam("lastname") String lastname,
                              @FormParam("password") String password,
                              @FormParam("email") String email) throws InstantiationException, IllegalAccessException,
                                                                       ClassNotFoundException, UsernameAlreadyTakenException, ExistingEmailException {
        guestService.createGuest(username, firstname, lastname, password,
                                 email, Guest.RegistrationMethod.REGISTRATION_METHOD_FORM, false);

        return Response.ok("User " + username + " was successfully created").build();

    }

    @DELETE
    @Secured({ "ROLE_ADMIN" })
    @Path("/{username}")
    public Response deleteGuest(@PathParam("username") String username)
            throws Exception {
        guestService.eraseGuestInfo(username);
        return Responses.noContent().build();

    }

    @GET
    @Path("/list")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response list() throws InstantiationException, IllegalAccessException,
                                ClassNotFoundException {
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
        return Response.ok(array.toString()).build();
    }

    @GET
    @Path("/{username}/roles")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getRoles(@PathParam("username") String username)
            throws InstantiationException, IllegalAccessException,
                   ClassNotFoundException {
        Guest guest = guestService.getGuest(username);
        JSONArray array = getGuestRolesJsonArray(guest);
        return Response.ok(array.toString()).build();
    }

    @POST
    @Path("/{username}/roles")
    @Secured({ "ROLE_ADMIN" })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response setRoles(@PathParam("username") String username,
                           @FormParam("roles") String roles) throws InstantiationException,
                                                                    IllegalAccessException, ClassNotFoundException {
        Guest guest = guestService.getGuest(username);
        StringTokenizer st = new StringTokenizer(roles, ",");
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
        return Response.ok(array.toString()).build();
    }

    @DELETE
    @Secured({ "ROLE_ADMIN" })
    @Path("/{username}/roles/{role}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response revokeRole(@PathParam("username") String username,
                             @PathParam("role") String role) throws InstantiationException,
                                                                    IllegalAccessException, ClassNotFoundException {
        Guest guest = guestService.getGuest(username);
        guestService.removeRole(guest.getId(), role);
        return Responses.noContent().build();
    }

    private JSONArray getGuestRolesJsonArray(Guest guest) {
        JSONArray array = new JSONArray();
        List<String> userRoles = guest.getUserRoles();
        for (String userRole : userRoles)
            array.add(userRole);
        return array;
    }

}
