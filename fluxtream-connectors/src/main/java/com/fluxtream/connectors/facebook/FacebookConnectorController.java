package com.fluxtream.connectors.facebook;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fluxtream.auth.AuthHelper;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SystemService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;

@Controller
@RequestMapping(value = "/facebook")
public class FacebookConnectorController {

	@Autowired
	Configuration env;
	
	@Autowired
	SystemService systemService;
	
	@Autowired
	GuestService guestService;

	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException{
		
		String redirectUri = env.get("facebook.redirect_uri");
		String clientId = env.get("facebook.client.id");

		String authorizeUrl = "https://www.facebook.com/dialog/oauth?" +
			"redirect_uri=" + redirectUri +
			"&client_id=" + clientId +
			"&scope=read_stream,user_activities,user_birthday," +
			"user_checkins,user_events,user_location,user_notes," +
			"user_online_presence,user_photos,user_relationships," +
			"user_videos,user_work_history";
		
		return "redirect:" + authorizeUrl;
	}
	
	@RequestMapping(value = "/swapToken")
	public ModelAndView upgradeToken(HttpServletRequest request) throws IOException, UnexpectedHttpResponseCodeException {
		
		String code = request.getParameter("code");
		String redirectUri = env.get("facebook.redirect_uri");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("code", code);
		params.put("client_id", env.get("facebook.client.id"));
		params.put("client_secret", env.get("facebook.client.secret"));
		params.put("redirect_uri", redirectUri);
		
		boolean hasError = false;
		String errorMessage = "non so bene";
		String fetched = "";
		try {
			String swapTokenUrl = "https://graph.facebook.com/oauth/access_token?" +
				"client_id=" + params.get("client_id") + "&redirect_uri=" + redirectUri +
				"&client_secret=" + params.get("client_secret") + "&code=" + code;
			fetched = HttpUtils.fetch(swapTokenUrl);
		} catch (RuntimeException e) {
			errorMessage = e.getMessage();
			hasError = true;
		}

		if (!hasError) {
			try {
				JSONObject errorWrapper = JSONObject.fromObject(fetched);
				JSONObject errorJson = errorWrapper.getJSONObject("error");
				if (errorJson!=null) {
					errorMessage = errorJson.getString("message");
				}
				hasError = true;
			} catch (Throwable t) {
				// we simply ignore errors parsing json -> it means we're good;
			}
		}
		
		if (!hasError) {

			Map<String,String> parameters = Utils.parseParameters(fetched);
			String access_token = parameters.get("access_token");
			String expires = parameters.get("expires");
			
			if (!access_token.equals("")) {
				Connector connector = Connector.getConnector("facebook");
				Guest guest = AuthHelper.getGuest();
            final ApiKey apiKey = guestService.createApiKey(guest.getId(), connector);

				guestService.setApiKeyAttribute(apiKey,
						"accessToken", access_token);
				guestService.setApiKeyAttribute(apiKey,
						"expires", expires);
				
				return new ModelAndView("redirect:/app/from/"+connector.getName());
			}
		}

		ModelAndView mav = new ModelAndView("error");
		mav.addObject("errorMessage", errorMessage);
		return mav;
	}
		
}
