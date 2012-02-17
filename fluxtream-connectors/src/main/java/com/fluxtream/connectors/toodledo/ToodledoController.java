package com.fluxtream.connectors.toodledo;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.domain.Guest;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.impl.ServiceUtils;

@Controller
@RequestMapping(value="/toodledo")
public class ToodledoController {

	@Autowired
	GuestService guestService;
		
	@Autowired
	Configuration env;
	
	@Autowired
	ToodledoUpdater updater;
	
	@RequestMapping(value = "/enterCredentials")
	public ModelAndView signin(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView(
				"connectors/toodledo/enterCredentials");
		return mav;
	}
	
	@RequestMapping(value="/submitCredentials")
	public ModelAndView setupToodledo(HttpServletRequest request, HttpServletResponse response)
		throws RateLimitReachedException, Exception
	{
		ModelAndView mav = new ModelAndView();
		String email = request.getParameter("username");
		String password = request.getParameter("password");
		email = email.trim();
		password = password.trim();
		request.setAttribute("username", email);
		List<String> required = new ArrayList<String>();
		if (email.equals(""))
			required.add("username");
		if (password.equals(""))
			required.add("password");
		if (required.size()!=0) {
			mav.setViewName("connectors/toodledo/enterCredentials");
			mav.addObject("required", required);
			return mav;
		}
		Guest guest = ServiceUtils.getCurrentGuest();
		String userid = updater.getToodledoUserid(guest.getId(), email, password);
		
		if (userid==null) {
			mav.setViewName("connectors/toodledo/error");
			return mav;
		}

		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("toodledo"), "email", email);
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("toodledo"), "password", password);
		guestService.setApiKeyAttribute(guest.getId(), Connector.getConnector("toodledo"), "userid", userid);
		
		mav.setViewName("connectors/toodledo/success");
		return mav;
	}
	
}
