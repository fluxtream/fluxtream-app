package org.fluxtream.connectors.moves;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.Configuration;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.controllers.ControllerSupport;
import org.fluxtream.connectors.updaters.UpdateFailedException;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.Notification;
import org.fluxtream.domain.metadata.FoursquareVenue;
import org.fluxtream.services.GuestService;
import org.fluxtream.services.JPADaoService;
import org.fluxtream.services.MetadataService;
import org.fluxtream.services.NotificationsService;
import org.fluxtream.utils.HttpUtils;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * User: candide
 * Date: 17/06/13
 * Time: 16:49
 */
@Controller
@RequestMapping(value = "/moves")
public class MovesController {

    @Autowired
    Configuration env;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    GuestService guestService;

    @Autowired
    JPADaoService jpaDaoService;

    static final Logger logger = Logger.getLogger(MovesController.class);


    @RequestMapping(value = "/oauth2/token")
    public String getToken(HttpServletRequest request) throws IOException, ServletException {

        String redirectUri = getRedirectUri();

        // Check that the redirectUri is going to work
        final String validRedirectUrl = env.get("moves.validRedirectURL");
        if (!validRedirectUrl.startsWith(ControllerSupport.getLocationBase(request, env))) {
            final long guestId = AuthHelper.getGuestId();
            final String validRedirectBase = getBaseURL(validRedirectUrl);
            notificationsService.addNamedNotification(guestId, Notification.Type.WARNING, Connector.getConnector("moves").statusNotificationName(),
                                                      "Adding a Moves connector only works when logged in through " + validRedirectBase +
                                                      ".  You are logged in through " + ControllerSupport.getLocationBase(request, env) +
                                                      ".<br>Please re-login via the supported URL or inform your Fluxtream administrator " +
                                                      "that the moves.validRedirectURL setting does not match your needs.");
            return "redirect:/app";
        }

        // Here we know that the redirectUri will work
        String approvalPageUrl = String.format("https://api.moves-app.com/oauth/v1/authorize?" +
                                               "redirect_uri=%s&" +
                                               "response_type=code&client_id=%s&" +
                                               "scope=activity location",
                                               redirectUri, env.get("moves.client.id"));
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
        // TODO: This should be checked against the moves.validRedirectURL property to make
        // sure that it will work.  Moves only accepts the specific redirect URI's which matches the one
        // configured for this key.
        return env.get("homeBaseUrl") + "moves/oauth2/swapToken";
    }


    @RequestMapping(value = "/oauth2/swapToken")
    public String swapToken(HttpServletRequest request) throws Exception {
        final String errorMessage = request.getParameter("error");
        final Guest guest = AuthHelper.getGuest();
        Connector connector = Connector.getConnector("moves");
        if (errorMessage!=null) {
            notificationsService.addNamedNotification(guest.getId(),
                                                      Notification.Type.ERROR, connector.statusNotificationName(),
                                                      "There was an error while setting you up with the moves service: " + errorMessage);
            return "redirect:/app";
        }
        final String code = request.getParameter("code");

        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("client_id", env.get("moves.client.id"));
        parameters.put("client_secret", env.get("moves.client.secret"));
        parameters.put("redirect_uri", getRedirectUri());
        final String json = HttpUtils.fetch("https://api.moves-app.com/oauth/v1/access_token", parameters);

        JSONObject token = JSONObject.fromObject(json);

        if (token.has("error")) {
            String errorCode = token.getString("error");
            notificationsService.addNamedNotification(guest.getId(),
                                                      Notification.Type.ERROR,
                                                      connector.statusNotificationName(),
                                                      errorCode);
            // NOTE: In the future if we implement renew for the Moves connector
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
            apiKey = guestService.createApiKey(guest.getId(), Connector.getConnector("moves"));
        }

        guestService.populateApiKey(apiKey.getId());
        guestService.setApiKeyAttribute(apiKey,
                                        "accessToken", token.getString("access_token"));
        guestService.setApiKeyAttribute(apiKey,
                                        "tokenExpires", String.valueOf(System.currentTimeMillis() + (token.getLong("expires_in")*1000)));
        guestService.setApiKeyAttribute(apiKey,
                                        "refreshToken", refresh_token);

        // Record that this connector is now up
        guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null);

        if (stateParameter !=null&&!StringUtils.isEmpty(stateParameter))
            return "redirect:/app/tokenRenewed/moves";
        else
            return "redirect:/app/from/moves";
    }

    String getAccessToken(final ApiKey apiKey) throws Exception {
        final String expiresString = guestService.getApiKeyAttribute(apiKey, "tokenExpires");
        long expires = Long.valueOf(expiresString);
        if (expires<System.currentTimeMillis())
            refreshToken(apiKey);
        return guestService.getApiKeyAttribute(apiKey, "accessToken");
    }

    private void refreshToken(final ApiKey apiKey) throws Exception, UpdateFailedException {
        // Check to see if we are running on a mirrored test instance
        // and should therefore refrain from swapping tokens lest we
        // invalidate an existing token instance
        String disableTokenSwap = env.get("disableTokenSwap");
        Connector connector = Connector.getConnector("moves");
        if(disableTokenSwap!=null && disableTokenSwap.equals("true")) {
            String msg = "**** Skipping refreshToken for moves connector instance because disableTokenSwap is set on this server";
                                            ;
            StringBuilder sb2 = new StringBuilder("module=MovesController component=MovesController action=refreshToken apiKeyId=" + apiKey.getId())
            			    .append(" message=\"").append(msg).append("\"");
            logger.info(sb2.toString());
            System.out.println(msg);

            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector.statusNotificationName(),
                                                      "Heads Up. This server cannot automatically refresh your Moves authentication tokens.<br>" +
                                                      "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                      "scroll to the Moves connector, delete the connector, and re-add<br>" +
                                                      "<p>We apologize for the inconvenience</p>");

            // Record permanent failure since this connector won't work again until
            // it is reauthenticated
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
            throw new UpdateFailedException("requires token reauthorization",true);
        }

        // We're not on a mirrored test server.  Try to swap the expired
        // access token for a fresh one.  Typically moves access tokens are good for
        // 180 days from time of issue.
        String swapTokenUrl = "https://api.moves-app.com/oauth/v1/access_token";

        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        Map<String,String> params = new HashMap<String,String>();
        params.put("refresh_token", refreshToken);
        params.put("client_id", guestService.getApiKeyAttribute(apiKey, "moves.client.id"));
        params.put("client_secret", guestService.getApiKeyAttribute(apiKey, "moves.client.secret"));
        params.put("grant_type", "refresh_token");

        String fetched;
        try {
            fetched = HttpUtils.fetch(swapTokenUrl, params);
            // Record that this connector is now up
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null);
        } catch (Exception e) {
            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector.statusNotificationName(),
                                                      "Heads Up. We failed in our attempt to automatically refresh your Moves authentication tokens.<br>" +
                                                      "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                      "scroll to the Moves connector, delete the connector, and re-add<br>" +
                                                      "<p>We apologize for the inconvenience</p>");

            // Record permanent update failure since this connector is never
            // going to succeed
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
            throw new UpdateFailedException("refresh token attempt failed", e, true);
        }

        JSONObject token = JSONObject.fromObject(fetched);
        final long expiresIn = token.getLong("expires_in");
        final String access_token = token.getString("access_token");

        final long now = System.currentTimeMillis();
        long tokenExpires = now + (expiresIn*1000);

        guestService.setApiKeyAttribute(apiKey,
                                        "accessToken", access_token);
        guestService.setApiKeyAttribute(apiKey,
                                        "tokenExpires", String.valueOf(tokenExpires));

    }

    @RequestMapping(value = "/place/{apiKeyId}/{id}")
    public void getMovesPlaceIcon(@PathVariable("apiKeyId") long apiKeyId,
                                  @PathVariable("id") long id,
                                  HttpServletResponse response) throws IOException {
        List l = jpaDaoService.executeNativeQuery("SELECT type, foursquareId FROM Facet_MovesPlace WHERE apiKeyId=(?1) AND id=(?2)", apiKeyId, id);
        if (l==null||l.size()==0)
            response.sendError(404);
        final Object[] singleResult = (Object[])l.get(0);
        String type = (String) singleResult[0];
        if (type.equals("foursquare")) {
            String foursquareId = (String) singleResult[1];
            final FoursquareVenue foursquareVenue = metadataService.getFoursquareVenue(foursquareId);
            response.sendRedirect(foursquareVenue.categoryIconUrlPrefix + "bg_32" + foursquareVenue.categoryIconUrlSuffix);
        } else {
            String homeBaseUrl = env.get("homeBaseUrl");
            response.sendRedirect(homeBaseUrl+"/images/moves/" + type + ".png");
        }
    }


}
