package com.fluxtream.connectors.bodytrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping(value = "/bodytrack")
public class BodytrackAuthController {

	@Autowired
	GuestService guestService;
		
	@Autowired
	Configuration env;
	
	@RequestMapping(value = "/enterCredentials")
	public ModelAndView signin(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView(
				"connectors/bodytrack/enterCredentials");
		mav.addObject("host", env.get("defaultBodytrackHost"));
		return mav;
	}
	
	@RequestMapping(value="/submitCredentials")
	public ModelAndView getBodytrackUserId(HttpServletRequest request, HttpServletResponse response)
		throws RateLimitReachedException, Exception
	{
		ModelAndView mav = new ModelAndView();
		String login = request.getParameter("username");
		String password = request.getParameter("password");
		String host = request.getParameter("host");
		login = login.trim();
		password = password.trim();
		host = host.trim();
		request.setAttribute("username", login);
		request.setAttribute("host", host);
		List<String> required = new ArrayList<String>();
		if (login.equals(""))
			required.add("username");
		if (password.equals(""))
			required.add("password");
		if (host.equals(""))
			required.add("host");
		if (required.size()!=0) {
			mav.setViewName("connectors/bodytrack/enterCredentials");
			mav.addObject("required", required);
			return mav;
		}
		Long guestId = getGuestId();
		
		Map<String,String> loginParams = new HashMap<String,String>();
		loginParams.put("login", login);
		loginParams.put("password", password);
		String jsonString = HttpUtils.fetch("http://" + host + "/login.json", loginParams, env);
		
		JSONObject json = JSONObject.fromObject(jsonString);
		if (json.has("fail")) {
			mav.setViewName("connectors/bodytrack/enterCredentials");
			mav.addObject("errorMessage", "Sorry, wrong credentials... Please try again.");
			return mav;
		} else {
			String user_id = json.getString("user_id");
			String name = json.getString("name");
			
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("bodytrack"), "login", login);
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("bodytrack"), "password", password);
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("bodytrack"), "user_id", user_id);
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("bodytrack"), "name", name);
			guestService.setApiKeyAttribute(guestId, Connector.getConnector("bodytrack"), "host", host);
			
			mav.setViewName("connectors/bodytrack/success");
			return mav;
		}
	}

	public static long getGuestId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		long guestId = ((FlxUserDetails)auth.getPrincipal()).getGuest().getId();
		return guestId;
	}

}
