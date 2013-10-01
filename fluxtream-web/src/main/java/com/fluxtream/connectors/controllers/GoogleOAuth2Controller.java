package com.fluxtream.connectors.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.OAuth2Helper;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification;
import com.fluxtream.mvc.controllers.ErrorController;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONObject;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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
        final String refreshTokenRemoveURL = guestService.getApiKeyAttribute(apiKey,"refreshTokenRemoveURL");
        oAuth2Helper.revokeRefreshToken(apiKey.getGuestId(), apiKey.getConnector(), refreshTokenRemoveURL);
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
            "access_type=offline";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public ModelAndView upgradeToken(HttpServletRequest request) throws IOException, UnexpectedHttpResponseCodeException {
		
		String swapTokenUrl = "https://accounts.google.com/o/oauth2/token";
		String code = request.getParameter("code");
		String redirectUri = ControllerSupport.getLocationBase(request, env) + "google/oauth2/swapToken";
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("google.client.id"));
		params.put("client_secret", env.get("google.client.secret"));
		params.put("redirect_uri", redirectUri);
		params.put("grant_type", "authorization_code");

		String fetched = HttpUtils.fetch(swapTokenUrl, params);
		
		JSONObject token = JSONObject.fromObject(fetched);
		
		String scope = (String) request.getSession().getAttribute("oauth2Scope");
		Connector scopedApi = systemService.getApiFromGoogleScope(scope);
		
		Guest guest = AuthHelper.getGuest();

        if (!token.has("refresh_token")) {
            String message = (new StringBuilder("<p>We couldn't get your oauth2 refresh token.</p>"))
                    .append("<p>Obviously, something went wrong.</p>")
                    .append("<p>You'll have to surf to your ")
                    .append("<a target='_new'  href='https://accounts.google.com/b/0/IssuedAuthSubTokens'>token mgmt page at Google's</a> ")
                    .append("and hit \"Revoke Access\" next to \"fluxtream — ").append(getGooglePrettyName(scopedApi)).append("\"</p>")
                    .append("<p>Then please, add the Google Latitude connector again.</p>")
                    .append("<p>We apologize for the inconvenience</p>").toString();
            notificationsService.addNotification(guest.getId(),
                                                 Notification.Type.ERROR,
                                                 message);
            return new ModelAndView("redirect:/app");
        }
        final String refresh_token = token.getString("refresh_token");
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
            // remove oauth1 keys if upgrading from previous connector version
            if (guestService.getApiKeyAttribute(apiKey, "googleConsumerKey")!=null)
                guestService.removeApiKeyAttribute(apiKey.getId(), "googleConsumerKey");
            if (guestService.getApiKeyAttribute(apiKey, "googleConsumerSecret")!=null)
                guestService.removeApiKeyAttribute(apiKey.getId(), "googleConsumerSecret");

            // now reset the connector to force a full reimport on google calendar
            if (apiKey.getConnector().getName().equals("google_calendar"))
               connectorUpdateService.flushUpdateWorkerTasks(apiKey, true);

        } else
            apiKey = guestService.createApiKey(guest.getId(), scopedApi);

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
