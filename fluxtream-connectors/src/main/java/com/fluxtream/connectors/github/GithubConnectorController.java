package com.fluxtream.connectors.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping(value = "/github")
public class GithubConnectorController {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException{
		
		String redirectUri = env.get("github.redirect_uri");
		String clientId = env.get("github.client.id");

		String authorizeUrl = "https://github.com/login/oauth/authorize?client_id=" + clientId +
			"&redirect_uri" + redirectUri +
			"&response_type=code&scope=user,public_repo,repo,gist";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public String upgradeToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		
		String swapTokenUrl = "https://github.com/login/oauth/access_token";
		String code = request.getParameter("code");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("github.client.id"));
		params.put("client_secret", env.get("github.client.secret"));
		params.put("redirect_uri", env.get("github.redirect_uri"));
		
		@SuppressWarnings("unused")
		String fetched = HttpUtils.fetch(swapTokenUrl, params, env);
		
		// this is a key-values string

		
		Connector connector = Connector.getConnector("github");

//		guestService.setApiKeyAttribute(guest.getId(), connector,
//				"accessToken", token.getString("access_token"));
//		guestService.setApiKeyAttribute(guest.getId(), connector,
//				"tokenExpires", String.valueOf(System.currentTimeMillis() + (token.getLong("expires_in")*1000)));
//		guestService.setApiKeyAttribute(guest.getId(), connector,
//				"refreshToken", token.getString("refresh_token"));
		
		return "redirect:/home/from/"+connector.getName();
	}
}
