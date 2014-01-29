package com.fluxtream.connectors.up;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.controllers.ControllerSupport;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.HttpUtils;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * User: candide
 * Date: 26/01/14
 * Time: 09:56
 */
@Controller
@RequestMapping(value = "/up")
public class JawboneUpController {

    @Autowired
    Configuration env;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    GuestService guestService;

    @RequestMapping(value = "/token")
    public String getToken(HttpServletRequest request) throws IOException, ServletException {

        String redirectUri = getRedirectUri();

        // Check that the redirectUri is going to work
        final String validRedirectUrl = env.get("jawboneUp.validRedirectURL");
        if (!validRedirectUrl.startsWith(ControllerSupport.getLocationBase(request, env))) {
            final long guestId = AuthHelper.getGuestId();
            final String validRedirectBase = getBaseURL(validRedirectUrl);
            notificationsService.addNamedNotification(guestId, Notification.Type.WARNING, Connector.getConnector("up").statusNotificationName(),
                                                      "Adding a Jawbone UP connector only works when logged in through " + validRedirectBase +
                                                      ".  You are logged in through " + ControllerSupport.getLocationBase(request, env) +
                                                      ".<br>Please re-login via the supported URL or inform your Fluxtream administrator " +
                                                      "that the jawboneUp.validRedirectURL setting does not match your needs.");
            return "redirect:/app";
        }

        // Here we know that the redirectUri will work
        String approvalPageUrl = String.format("https://jawbone.com/auth/oauth2/auth?" +
                                               "redirect_uri=%s&" +
                                               "response_type=code&client_id=%s&" +
                                               "scope=basic_read location_read move_read sleep_read",
                                               redirectUri, env.get("jawboneUp.client.id"));
        final String apiKeyIdParameter = request.getParameter("apiKeyId");
        if (apiKeyIdParameter !=null && !StringUtils.isEmpty(apiKeyIdParameter))
            approvalPageUrl += "&state=" + apiKeyIdParameter;

        return "redirect:" + approvalPageUrl;
    }

    public static String getBaseURL(String url) {
        try {
            URI uri = new URI(url);
            StringBuilder rootURI = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
            if(uri.getPort()!=-1) {
                rootURI.append(":" + uri.getPort());
            }
            return (rootURI.toString());
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    private String getRedirectUri() {
        // TODO: This should be checked against the jawboneUp.validRedirectURL property to make
        // sure that it will work.  UP only accepts the specific redirect URI's which matches the one
        // configured for this key.
        return env.get("homeBaseUrl") + "up/swapToken";
    }


    @RequestMapping(value = "/swapToken")
    public String swapToken(HttpServletRequest request) throws Exception {
        final String errorMessage = request.getParameter("error");
        final Guest guest = AuthHelper.getGuest();
        Connector connector = Connector.getConnector("up");
        if (errorMessage!=null) {
            notificationsService.addNamedNotification(guest.getId(),
                                                      Notification.Type.ERROR, connector.statusNotificationName(),
                                                      "There was an error while setting you up with the Jawbone UP service: " + errorMessage);
            return "redirect:/app";
        }
        final String code = request.getParameter("code");

        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("client_id", env.get("jawboneUp.client.id"));
        parameters.put("client_secret", env.get("jawboneUp.client.secret"));
        final String json = HttpUtils.fetch("https://jawbone.com/auth/oauth2/token", parameters);

        JSONObject token = JSONObject.fromObject(json);

        if (token.has("error")) {
            String errorCode = token.getString("error");
            notificationsService.addNamedNotification(guest.getId(),
                                                      Notification.Type.ERROR,
                                                      connector.statusNotificationName(),
                                                      errorCode);
            // NOTE: In the future if we implement renew for the UP connector
            // we will potentially need to mark the connector as permanently failed.
            // The way to do this is to get hold of the existing apiKey and do:
            //  guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
            return "redirect:/app";
        }

        final String refresh_token = token.getString("refresh_token");

        // Create the entry for this new apiKey in the apiKey table and populate
        // ApiKeyAttributes with all of the keys fro oauth.properties needed for
        // subsequent update of this connector instance.
        ApiKey apiKey;
        final String stateParameter = request.getParameter("state");
        if (stateParameter !=null&&!StringUtils.isEmpty(stateParameter)) {
            long apiKeyId = Long.valueOf(stateParameter);
            apiKey = guestService.getApiKey(apiKeyId);
        } else {
            apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("up"));
        }

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey,
                                        "accessToken", token.getString("access_token"));
        guestService.setApiKeyAttribute(apiKey,
                                        "tokenExpires", String.valueOf(System.currentTimeMillis() + DateTimeConstants.MILLIS_PER_DAY*365));
        guestService.setApiKeyAttribute(apiKey,
                                        "refreshToken", refresh_token);

        // Record that this connector is now up
        guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null);

        if (stateParameter !=null&&!StringUtils.isEmpty(stateParameter))
            return "redirect:/app/tokenRenewed/up";
        else
            return "redirect:/app/from/up";
    }

}
