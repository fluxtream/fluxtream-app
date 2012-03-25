package com.fluxtream.connectors.instagram;

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
@RequestMapping(value = "/instagram")
public class InstagramOAuth2Controller {

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
		String redirectUri = env.get("homeBaseUrl") + "instagram/swapToken";
		
		String clientId = env.get("instagram.client.id");

		String authorizeUrl = "https://api.instagram.com/oauth/authorize/?client_id=" + clientId + 
			"&redirect_uri=" + redirectUri +
			"&response_type=code";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		String swapTokenUrl = "https://api.instagram.com/oauth/access_token";
		String code = request.getParameter("code");
		String redirectUri = env.get("homeBaseUrl") + "instagram/swapToken";
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("instagram.client.id"));
		params.put("client_secret", env.get("instagram.client.secret"));
		params.put("redirect_uri", redirectUri);
		params.put("grant_type", "authorization_code");
		
		String fetched = HttpUtils.fetch(swapTokenUrl, params, env);
		
		JSONObject token = JSONObject.fromObject(fetched);
		
//		String scope = (String) request.getSession().getAttribute("oauth2Scope");
		Guest guest = ControllerHelper.getGuest();

		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("INSTAGRAM"),
				"accessToken", token.getString("access_token"));
		
		JSONObject userObject = token.getJSONObject("user");
		
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("INSTAGRAM"),
			"id", userObject.getString("id"));
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("INSTAGRAM"),
			"username", userObject.getString("username"));
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("INSTAGRAM"),
			"full_name", userObject.getString("full_name"));
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("INSTAGRAM"),
			"profile_picture", userObject.getString("profile_picture"));

		return "redirect:/app/from/"+Connector.getConnector("INSTAGRAM").getName();
	}
}
