package com.fluxtream.connectors.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
