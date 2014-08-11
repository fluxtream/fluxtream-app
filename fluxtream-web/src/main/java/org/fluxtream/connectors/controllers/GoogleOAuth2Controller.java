package org.fluxtream.connectors.controllers;

import net.sf.json.JSONObject;
import org.codehaus.plexus.util.ExceptionUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.OAuth2Helper;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ConnectorUpdateService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.services.SystemService;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.mvc.controllers.ErrorController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/google/oauth2")
public class GoogleOAuth2Controller {

    FlxLogger logger = FlxLogger.getLogger(GoogleOAuth2Controller.class);

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    @Autowired
    OAuth2Helper oAuth2Helper;

    @Autowired
    ErrorController errorController;

    private final static String APIKEYID_ATTRIBUTE = "google.oauth2.apiKeyId";

    @RequestMapping(value = "/{apiKeyId}/token")
    public String renewToken(@PathVariable("apiKeyId") String apiKeyId, HttpServletRequest request) throws IOException, ServletException {
        request.getSession().setAttribute(APIKEYID_ATTRIBUTE, apiKeyId);
        final ApiKey apiKey = guestService.getApiKey(Long.valueOf(apiKeyId));

        // Check if the stored client ID matches the one in our properties file, and only in that
        // case try to revoke.  TODO: in that case skip next two lines
        String propertiesClientId = env.get("google.client.id");
        String storedClientId = guestService.getApiKeyAttribute(apiKey,"google.client.id");

        if(propertiesClientId !=null && storedClientId!=null && propertiesClientId.equals(storedClientId)) {
            // Renewing token for the same server, do revoke attempt first
            final String refreshTokenRemoveURL = guestService.getApiKeyAttribute(apiKey,"refreshTokenRemoveURL");
            oAuth2Helper.revokeRefreshToken(apiKey.getGuestId(), apiKey.getConnector(), refreshTokenRemoveURL);

            String msg="Called revokeRefreshToken with refreshTokenRemoveURL='" + refreshTokenRemoveURL + "'";
            StringBuilder sb = new StringBuilder("module=GoogleOauth2Controller component=renewToken action=renewToken apiKeyId=" + apiKeyId)
                    .append(" guestId=").append(apiKey.getGuestId())
                    .append(" message=\"").append(msg).append("\"");
            logger.info(sb.toString());
        }
        else {
            String msg="Skipped revokeRefreshToken, stored and properties google.client.id do not match (" + storedClientId + " vs " + propertiesClientId + ")" ;
                        StringBuilder sb = new StringBuilder("module=GoogleOauth2Controller component=renewToken action=renewToken apiKeyId=" + apiKeyId)
                                .append(" guestId=").append(apiKey.getGuestId())
                                .append(" message=\"").append(msg).append("\"");
                        logger.info(sb.toString());
        }
        // Continue on to ask user for authorization whether or not you did a revoke
        return getToken(request);
    }

    @RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request) throws IOException, ServletException{
		
		String scope = request.getParameter("scope");
		request.getSession().setAttribute("oauth2Scope", scope);
		String redirectUri = ControllerSupport.getLocationBase(request, env) + "google/oauth2/swapToken";
		String clientId = env.get("google.client.id");

		String authorizeUrl = "https://accounts.google.com/o/oauth2/auth?client_id=" + clientId +
			"&redirect_uri=" + redirectUri +
			"&scope=" + scope +
			"&response_type=code&" +
            "access_type=offline&" +
            "approval_prompt=force";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public ModelAndView upgradeToken(HttpServletRequest request) throws IOException, UnexpectedHttpResponseCodeException {

        String scope = (String) request.getSession().getAttribute("oauth2Scope");
        Connector scopedApi = systemService.getApiFromGoogleScope(scope);

        Guest guest = AuthHelper.getGuest();

        String errorParameter = request.getParameter("error");
        if (errorParameter!=null) {
            notificationsService.addNamedNotification(guest.getId(), Notification.Type.WARNING,
                                                      scopedApi.statusNotificationName(),
                                                      "There was a problem importing your " + scopedApi.prettyName() + " data: " + errorParameter);
            return new ModelAndView("redirect:/app/");
        }

		String swapTokenUrl = "https://accounts.google.com/o/oauth2/token";
		String code = request.getParameter("code");
		String redirectUri = ControllerSupport.getLocationBase(request, env) + "google/oauth2/swapToken";
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("google.client.id"));
		params.put("client_secret", env.get("google.client.secret"));
		params.put("redirect_uri", redirectUri);
		params.put("grant_type", "authorization_code");

      // Get the google branding info.  Default to fluxtream if not set, but can override in
      // oauth.properties by setting the default google.client.brandName parameter
      String brandName = env.get("google.client.brandName");
      if(brandName == null) {
         // Not set in oauth.properties file, default to "Fluxtream"
         brandName="Fluxtream";
      }

      // Try to renew the token.  On failure leave token=null
      JSONObject token = null;

      try {
         String fetched = HttpUtils.fetch(swapTokenUrl, params);
         token = JSONObject.fromObject(fetched);
      } catch(Throwable e) {
         token = null;
      }

        ApiKey apiKey;
        final boolean isRenewToken = request.getSession().getAttribute(APIKEYID_ATTRIBUTE) != null;
        if (isRenewToken) {
            String apiKeyId = (String)request.getSession().getAttribute(APIKEYID_ATTRIBUTE);
            apiKey = guestService.getApiKey(Long.valueOf(apiKeyId));
            if (apiKey==null) {
                Exception e = new Exception();
                String stackTrace = ExceptionUtils.getStackTrace(e);
                String errorMessage = "no apiKey with id '%s'... It looks like you are trying to renew the tokens of a non-existing Connector (/ApiKey)";
                return errorController.handleError(500, errorMessage, stackTrace);
            }

            if (token == null || !token.has("refresh_token")) {
                String message = (new StringBuilder("<p>We couldn't get your oauth2 refresh token.  "))
                        .append("Something went wrong.</p>")
                        .append("<p>You'll have to surf to your ")
                        .append("<a target='_new'  href='https://accounts.google.com/b/0/IssuedAuthSubTokens'>token mgmt page at Google</a> ")
                        .append("and hit \"Revoke Access\" next to \"").append(brandName).append(" — ").append(getGooglePrettyName(scopedApi)).append("\"</p>")
    		              .append("<p>Then please, head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a> ")
    		              .append("and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)</p>")
                        .append("<p>We apologize for the inconvenience</p>").toString();

                notificationsService.addNamedNotification(guest.getId(),
                                                     Notification.Type.ERROR,
                                                     apiKey.getConnector().statusNotificationName(),
                                                     message);
                // Record permanent failure since this connector won't work again until
                // it is reauthenticated
                guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
                return new ModelAndView("redirect:/app");
            }

            // Remove oauth1 keys if upgrading from previous connector version.
            // Remember whether or not we're upgrading from previous connector version.
            // If so, do a full history update.  Otherwise don't force a full
            // history update and allow the update to be whatever it normally would be
            boolean upgradeFromOauth1 = false;

            if (guestService.getApiKeyAttribute(apiKey, "googleConsumerKey")!=null) {
                guestService.removeApiKeyAttribute(apiKey.getId(), "googleConsumerKey");
                upgradeFromOauth1 = true;
            }
            if (guestService.getApiKeyAttribute(apiKey, "googleConsumerSecret")!=null) {
                guestService.removeApiKeyAttribute(apiKey.getId(), "googleConsumerSecret");
                upgradeFromOauth1 = true;
            }

            // If upgradeFromOauth1 reset the connector to force a full reimport on google calendar,
            // otherwise just do a normal update
            if (apiKey.getConnector().getName().equals("google_calendar")) {
                connectorUpdateService.flushUpdateWorkerTasks(apiKey, upgradeFromOauth1);
            }

        } else {
            apiKey = guestService.createApiKey(guest.getId(), scopedApi);
        }

        // We need to store google.client.id and google.client.secret with the
        // apiKeyAttributes in either the case of original creation of the key
        // or token renewal.  createApiKey actually handles the former case, but
        // not the latter.  Do it in all cases here.
        guestService.setApiKeyAttribute(apiKey, "google.client.id", env.get("google.client.id"));
        guestService.setApiKeyAttribute(apiKey, "google.client.secret", env.get("google.client.secret"));

        final String refresh_token = token.getString("refresh_token");

        guestService.setApiKeyAttribute(apiKey,
				"accessToken", token.getString("access_token"));
		guestService.setApiKeyAttribute(apiKey,
				"tokenExpires", String.valueOf(System.currentTimeMillis() + (token.getLong("expires_in")*1000)));
        guestService.setApiKeyAttribute(apiKey,
				"refreshToken", refresh_token);
        final String encodedRefreshToken = URLEncoder.encode(refresh_token, "UTF-8");
        guestService.setApiKeyAttribute(apiKey,
                                        "refreshTokenRemoveURL",
                                        "https://accounts.google.com/o/oauth2/revoke?token="
                                        + encodedRefreshToken);

        // Record this connector as having status up
        guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_UP, null);
        // Schedule an update for this connector
        connectorUpdateService.updateConnector(apiKey, false);

        if (isRenewToken) {
            request.getSession().removeAttribute(APIKEYID_ATTRIBUTE);
            return new ModelAndView("redirect:/app/tokenRenewed/"+scopedApi.getName());
        }
        return new ModelAndView("redirect:/app/from/"+scopedApi.getName());
    }

    private Object getGooglePrettyName(final Connector scopedApi) {
        if (scopedApi.getName().equals("google_latitude"))
            return "Latitude";
        else if (scopedApi.getName().equals("google_calendar"))
            return "Google Calendar (or Google Agenda)";
        else {
            logger.warn("Please check Google's pretty name for API " + scopedApi.getName() + " (see GoogleOAuth2Controller.java)");
            return scopedApi.prettyName();
        }
    }
}
