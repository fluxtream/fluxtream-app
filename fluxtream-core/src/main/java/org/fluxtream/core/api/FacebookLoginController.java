package org.fluxtream.core.api;

import com.google.gson.Gson;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.impl.ExistingEmailException;
import org.fluxtream.core.services.impl.UsernameAlreadyTakenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.fluxtream.core.utils.Utils.generateSecureRandomString;

/**
 * User: candide
 * Date: 09/09/13
 * Time: 12:13
 */
@Path("/v1/facebook")
@Component("RESTFacebookLoginController")
@Scope("request")
public class FacebookLoginController {

    @Autowired
    Configuration env;

    @Autowired
    GuestService guestService;

    Gson gson = new Gson();

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/login")
    public Response facebookLogin(@QueryParam("access_token") String access_token) {
        String appId = env.get("facebook.appId");
        String appSecret = env.get("facebook.appSecret");
        try {
            FacebookClient facebookClient = new DefaultFacebookClient(access_token);
            User user = facebookClient.fetchObject("me", User.class);

            String me = "";
            try {
                me = gson.toJson(user);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Guest guest = guestService.getGuest(user.getUsername());
            final String autoLoginToken = generateSecureRandomString();
            if (guest==null) {
                FacebookClient.AccessToken accessToken =
                        new DefaultFacebookClient().obtainExtendedAccessToken(appId, appSecret, access_token);
                String firstname = user.getFirstName();
                String lastname = user.getLastName();
                guest = guestService.createGuest(user.getUsername(), firstname!=null?firstname:"",
                                                 lastname!=null?lastname:"",
                                                 null, user.getEmail(),
                                                 Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK, null);
                final ApiKey apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("facebook"));

                guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken.getAccessToken());
                guestService.setApiKeyAttribute(apiKey, "expires", String.valueOf(accessToken.getExpires().getTime()));
                guestService.setApiKeyAttribute(apiKey, "me", me);

                final String message = "Facebook guest creation success!";
                return Response.ok(getStatusModel(guest, autoLoginToken, message)).build();
            } else {
                final String message = "Facebook auto-login success!";
                return Response.ok(getStatusModel(guest, autoLoginToken, message)).build();

            }
        } catch(ExistingEmailException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("We already have a user under this email address.").build();
        } catch(UsernameAlreadyTakenException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Sorry, this username is already taken").build();
        }
    }

    private StatusModel getStatusModel(final Guest guest, final String autoLoginToken, final String message) {
        guestService.setAutoLoginToken(guest.getId(), autoLoginToken);
        final StatusModel result = new StatusModel(true, message);
        result.payload = autoLoginToken;
        return result;
    }
}
