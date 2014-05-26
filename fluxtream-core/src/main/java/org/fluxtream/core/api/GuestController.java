package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.auth.CoachRevokedException;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.CoachingBuddy;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.mvc.models.guest.GuestModel;
import org.fluxtream.core.services.CoachingService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fluxtream.core.utils.Utils.hash;

@Path("/guest")
@Component("RESTGuestController")
@Api(value = "/guest", description = "Retrieve guest information")
@Scope("request")
public class GuestController {

	@Autowired
	GuestService guestService;

	Gson gson = new Gson();

	@Autowired
	Configuration env;

    @Autowired
    CoachingService coachingService;

    @GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
    @ApiOperation(value = "Retrieve information on the currently logged in's guest", response = GuestModel.class)
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
    @ApiOperation(value = "Retrieve the avatar (gravatar) of the currently logged in's guest", response = String.class)
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
    @Path("/coachees")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Retrieve the currently logged in guest's list of coachees", responseContainer = "array",
            response = GuestModel.class)
    public List<GuestModel> getCoachees() {
        Guest guest = AuthHelper.getGuest();
        final List<Guest> coachees = coachingService.getCoachees(guest.getId());
        final List<GuestModel> coacheeModels = new ArrayList<GuestModel>();
        for (Guest coachee : coachees)
            coacheeModels.add(new GuestModel(coachee));
        return coacheeModels;
    }

}
