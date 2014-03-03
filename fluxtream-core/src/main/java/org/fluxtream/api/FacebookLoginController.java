package org.fluxtream.api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.fluxtream.Configuration;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.mvc.models.StatusModel;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.impl.ExistingEmailException;
import org.fluxtream.services.impl.UsernameAlreadyTakenException;
import com.google.gson.Gson;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.fluxtream.utils.Utils.generateSecureRandomString;

/**
 * User: candide
 * Date: 09/09/13
 * Time: 12:13
 */
@Path("/facebook")
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
    public StatusModel facebookLogin(@QueryParam("access_token") String access_token) {
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
                                                 Guest.RegistrationMethod.REGISTRATION_METHOD_FACEBOOK);
                final ApiKey apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("facebook"));

                guestService.setApiKeyAttribute(apiKey, "accessToken", accessToken.getAccessToken());
                guestService.setApiKeyAttribute(apiKey, "expires", String.valueOf(accessToken.getExpires().getTime()));
                guestService.setApiKeyAttribute(apiKey, "me", me);

                final String message = "Facebook guest creation success!";
                return getStatusModel(guest, autoLoginToken, message);
            } else {
                final String message = "Facebook auto-login success!";
                return getStatusModel(guest, autoLoginToken, message);

            }
        } catch(ExistingEmailException e) {
            return new StatusModel(false, "We already have a user under this email address.");
        } catch(UsernameAlreadyTakenException e) {
            return new StatusModel(false, "Sorry, this username is already taken");
        }
    }

    private StatusModel getStatusModel(final Guest guest, final String autoLoginToken, final String message) {
        guestService.setAutoLoginToken(guest.getId(), autoLoginToken);
        final StatusModel result = new StatusModel(true, message);
        result.payload = autoLoginToken;
        return result;
    }
}
