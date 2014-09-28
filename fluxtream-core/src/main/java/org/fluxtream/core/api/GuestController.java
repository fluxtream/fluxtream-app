package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.*;
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
import org.fluxtream.core.mvc.models.AvatarImageModel;
import org.fluxtream.core.mvc.models.guest.GuestModel;
import org.fluxtream.core.services.BuddiesService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fluxtream.core.utils.Utils.hash;

@Path("/v1/guest")
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
    BuddiesService buddiesService;

    @GET
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
    @ApiResponses({
            @ApiResponse(code=401, message="The user is no longer logged in"),
            @ApiResponse(code=403, message="Buddy-to-access authorization has been revoked")
    })
    @ApiOperation(value = "Retrieve information on the currently logged in's guest", response = GuestModel.class)
	public Response getCurrentGuest(@ApiParam(value="Include the guest's avatar?") @QueryParam("includeAvatar") final boolean includeAvatar,
                                    @ApiParam(value="Buddy to access username parameter (" + BuddiesService.BUDDY_TO_ACCESS_PARAM + ")", required=false) @QueryParam(BuddiesService.BUDDY_TO_ACCESS_PARAM) String buddyToAccessParameter) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        try{
            CoachingBuddy coachee;
            try { coachee = AuthHelper.getCoachee(buddyToAccessParameter, buddiesService);
            } catch (CoachRevokedException e) {return Response.status(403).entity("Sorry, permission to access this data has been revoked. Please reload your browser window").build();}
            Guest guest = ApiHelper.getBuddyToAccess(guestService, coachee);
            if (guest==null)
                return Response.status(401).entity("You are no longer logged in").build();
            GuestModel guestModel = new GuestModel(guest, coachee!=null);
            if (includeAvatar)
                guestModel.avatar = getAvatarImageModel(buddyToAccessParameter, guest);
            return Response.ok(guestModel).build();
        }
        catch (Exception e){
            return Response.serverError().entity("Failed to get current guest: " + e.getMessage()).build();
        }
	}

    @GET
    @Path("/avatarImage")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiResponses({
            @ApiResponse(code=401, message="The user is no longer logged in"),
            @ApiResponse(code=403, message="Buddy-to-access authorization has been revoked")
    })
    @ApiOperation(value = "Retrieve the avatar (gravatar) of the currently logged in's guest", response = AvatarImageModel.class)
    public Response getAvatarImage(@ApiParam(value="Buddy to access username parameter (" + BuddiesService.BUDDY_TO_ACCESS_PARAM + ")", required=false) @QueryParam(BuddiesService.BUDDY_TO_ACCESS_PARAM) String buddyToAccessParameter) {
        Guest guest = AuthHelper.getGuest();
        AvatarImageModel avatarImage = getAvatarImageModel(buddyToAccessParameter, guest);
        return Response.ok(avatarImage).build();
    }

    private AvatarImageModel getAvatarImageModel(String buddyToAccessParameter, Guest guest) {
        AvatarImageModel avatarImage = new AvatarImageModel();
        String type = "none";
        String url;
        try {
            final CoachingBuddy coachee = AuthHelper.getCoachee(buddyToAccessParameter, buddiesService);
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
        avatarImage.type = type;
        avatarImage.url = url;
        return avatarImage;
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
    @ApiOperation(value = "Retrieve the currently logged in guest's list of coachees", responseContainer = "Array",
            response = GuestModel.class)
    public List<GuestModel> getCoachees() {
        Guest guest = AuthHelper.getGuest();
        final List<Guest> coachees = buddiesService.getTrustedBuddies(guest.getId());
        final List<GuestModel> coacheeModels = new ArrayList<GuestModel>();
        for (Guest coachee : coachees)
            coacheeModels.add(new GuestModel(coachee, true));
        return coacheeModels;
    }

}
