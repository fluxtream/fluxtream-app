package com.fluxtream.connectors.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fluxtream.domain.Notification;
import com.fluxtream.services.NotificationsService;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping(value = "/google/oauth2")
public class GoogleOAuth2Controller {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

    @Autowired
    NotificationsService notificationsService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException{
		
		String scope = request.getParameter("scope");
		request.getSession().setAttribute("oauth2Scope", scope);
		String redirectUri = env.get("homeBaseUrl") + "google/oauth2/swapToken";
		
		String clientId = env.get("google.client.id");

		String authorizeUrl = "https://accounts.google.com/o/oauth2/auth?client_id=" + clientId +
			"&redirect_uri=" + redirectUri +
			"&scope=" + scope +
			"&response_type=code&" +
            "access_type=offline";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		String swapTokenUrl = "https://accounts.google.com/o/oauth2/token";
		String code = request.getParameter("code");
		String redirectUri = env.get("homeBaseUrl") + "google/oauth2/swapToken";
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("google.client.id"));
		params.put("client_secret", env.get("google.client.secret"));
		params.put("redirect_uri", redirectUri);
		params.put("grant_type", "authorization_code");

		String fetched = HttpUtils.fetch(swapTokenUrl, params, env);
		
		JSONObject token = JSONObject.fromObject(fetched);
		
		String scope = (String) request.getSession().getAttribute("oauth2Scope");
		Connector scopedApi = systemService.getApiFromGoogleScope(scope);
		
		Guest guest = ControllerHelper.getGuest();

        if (!token.has("refresh_token")) {
            String message = (new StringBuilder("<p>We couldn't get your oauth2 refresh token.</p>"))
                    .append("<p>Obviously, something went wrong.</p>")
                    .append("<p>You'll have to surf to your ")
                    .append("<a target='_new'  href='https://accounts.google.com/b/0/IssuedAuthSubTokens'>token mgmt page at Google's</a> ")
                    .append("and hit \"Revoke Access\" next to \"fluxtream — Google Latitude\"</p>")
                    .append("<p>Then please, add the Google Latitude connector again./p>")
                    .append("<p>We apologize for the inconvenience</p>").toString();
            notificationsService.addNotification(guest.getId(),
                                                 Notification.Type.ERROR,
                                                 message);
            return "redirect:/app";
        }
        final String refresh_token = token.getString("refresh_token");

        guestService.setApiKeyAttribute(guest.getId(), scopedApi,
				"accessToken", token.getString("access_token"));
		guestService.setApiKeyAttribute(guest.getId(), scopedApi,
				"tokenExpires", String.valueOf(System.currentTimeMillis() + (token.getLong("expires_in")*1000)));
        guestService.setApiKeyAttribute(guest.getId(), scopedApi,
				"refreshToken", refresh_token);
        guestService.setApiKeyAttribute(guest.getId(), scopedApi,
                                        "refreshTokenRemoveURL",
                                        "https://accounts.google.com/o/oauth2/revoke?token="
                                        + refresh_token);

        return "redirect:/app/from/"+scopedApi.getName();
    }
}
