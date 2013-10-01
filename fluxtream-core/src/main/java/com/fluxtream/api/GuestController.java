package com.fluxtream.api;

import java.io.IOException;
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
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.auth.CoachRevokedException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.mvc.models.guest.GuestModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.impl.ExistingEmailException;
import com.fluxtream.services.impl.UsernameAlreadyTakenException;
import com.fluxtream.utils.HttpUtils;
import com.google.gson.Gson;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.Utils.hash;

@Path("/guest")
@Component("RESTGuestController")
@Scope("request")
public class GuestController {

	@Autowired
	GuestService guestService;

	Gson gson = new Gson();

	@Autowired
	Configuration env;

    @GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getCurrentGuest() throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        try{
            long guestId = AuthHelper.getGuestId();

            Guest guest = guestService.getGuestById(guestId);
            GuestModel guestModel = new GuestModel(guest);

            return gson.toJson(guestModel);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Failed to get current guest: " + e.getMessage()));
        }
	}

    @GET
    @Path("/avatarImage")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAvatarImage() {
        Guest guest = AuthHelper.getGuest();
        JSONObject json = new JSONObject();
        String type = "none";
        String url;
        try {
            final CoachingBuddy coachee = AuthHelper.getCoachee();
            if (coachee!=null)
                guest = guestService.getGuestById(coachee.guestId);
        }
        catch (CoachRevokedException e) {}
        if (guest.registrationMethod == Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK||
            guest.registrationMethod == Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK_WITH_PASSWORD) {
            url = getFacebookAvatarImageURL(guest);
            if (url!=null)
                type = "facebook";
        } else {
            url = getGravatarImageURL(guest);
            if (url!=null)
                type = "gravatar";
        }
        json.put("type", type);
        json.put("url", url);
        final String jsonString = json.toString();
        return jsonString;
    }

    private String getGravatarImageURL(Guest guest) {
        String emailHash = hash(guest.email.toLowerCase().trim()); //gravatar specifies the email should be trimmed, taken to lowercase, and then MD5 hashed
        String gravatarURL = String.format("http://www.gravatar.com/avatar/%s?s=27&d=404", emailHash);
        HttpGet get = new HttpGet(gravatarURL);
        int res = 0;
        try { res = ((new DefaultHttpClient()).execute(get)).getStatusLine().getStatusCode(); }
        catch (IOException e) {e.printStackTrace();}
        return res==200 ? gravatarURL : null;
    }

    public String getFacebookAvatarImageURL(Guest guest) {
        final ApiKey facebook = guestService.getApiKey(guest.getId(), Connector.getConnector("facebook"));
        final String meString = guestService.getApiKeyAttribute(facebook, "me");
        JSONObject meJSON = JSONObject.fromObject(meString);
        final String facebookId = meJSON.getString("id");
        try {
            String avatarURL = String.format("http://graph.facebook.com/%s/picture?type=small&redirect=false&return_ssl_resources=true", facebookId);
            String jsonString = HttpUtils.fetch(avatarURL);
            JSONObject json = JSONObject.fromObject(jsonString);
            if (json.has("data")) {
                json = json.getJSONObject("data");
                if (json.has("url")&&json.has("is_silhouette")) {
                    if (!json.getBoolean("is_silhouette")) {
                        return json.getString("url");
                    }
                }
            }
        } catch (Throwable t){
            t.printStackTrace();
        }
        return null;
    }

    @GET
	@Path("/{connector}/oauthTokens")
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
	@Produces({ MediaType.APPLICATION_JSON })
	public String createGuest(@FormParam("username") String username,
			@FormParam("firstname") String firstname,
			@FormParam("lastname") String lastname,
			@FormParam("password") String password,
			@FormParam("email") String email) throws InstantiationException, IllegalAccessException,
                                                     ClassNotFoundException, UsernameAlreadyTakenException,
                                                     ExistingEmailException {
		try {
			guestService.createGuest(username, firstname, lastname, password,
					email, Guest.RegistrationMethod.REGISTRATION_METHOD_FORM);
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
