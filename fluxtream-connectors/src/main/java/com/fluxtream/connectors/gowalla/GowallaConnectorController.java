package com.fluxtream.connectors.gowalla;

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
@RequestMapping(value = "/gowalla")
public class GowallaConnectorController {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException{
		
		String redirectUri = env.get("gowalla.redirect_uri");
		String clientId = env.get("gowalla.client.id");

		String authorizeUrl = "https://gowalla.com/api/oauth/new?" +
			"redirect_uri=" + redirectUri +
			"&client_id=" + clientId +
			"&type=code";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		String swapTokenUrl = "https://api.gowalla.com/api/oauth/token";
		String code = request.getParameter("code");
		String redirectUri = env.get("gowalla.redirect_uri");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("gowalla.client.id"));
		params.put("client_secret", env.get("gowalla.client.secret"));
		params.put("grant_type", "authorization_code");
		params.put("redirect_uri", redirectUri);
		
		String fetched = HttpUtils.fetch(swapTokenUrl, params, env);
		
		JSONObject token = JSONObject.fromObject(fetched);
		Connector connector = Connector.getConnector("gowalla");
		Guest guest = ControllerHelper.getGuest();

		guestService.setApiKeyAttribute(guest.getId(), connector,
				"accessToken", token.getString("access_token"));
		
		return "redirect:/home/from/"+connector.getName();
	}
}
