package com.fluxtream.mvc.controllers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.SecurityUtils;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;

@Controller
public class NavController {
	static Logger logger = Logger.getLogger(NavController.class);

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

	@Autowired
	ApiDataService apiDataService;
	
	@Autowired
	MetadataService metadataService;

	@Autowired
	NotificationsService notificationsService;

	@Autowired
	BeanFactory beanFactory;

	Gson gson = new Gson();

	@RequestMapping(value = "/log")
	public ModelAndView welcomeApp(HttpServletRequest request, Principal p)
			throws IOException, NoSuchAlgorithmException {

		long guestId = ControllerHelper.getGuestId();
		String remoteAddr = request.getHeader("X-Forwarded-For");
		if (remoteAddr == null)
			remoteAddr = request.getRemoteAddr();
		guestService.checkIn(guestId, remoteAddr);
		initializeWithTimeZone(request, guestId);
		
		return log(request, p);
	}

	@RequestMapping(value = "/library")
	public ModelAndView notSupported(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("library");
		mav.addObject("release", env.get("release"));
		return mav;
	}

	@RequestMapping(value = "/log/date/{date}")
	public ModelAndView homeDate(HttpServletRequest request, Principal p,
			@PathVariable String date) throws IOException, NoSuchAlgorithmException {
		if (date == null || date.equals("null"))
			return null;
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		homeModel.setDate(date);
		return log(request, p);
	}
	
	public ModelAndView log(HttpServletRequest request, Principal p)
			throws IOException, NoSuchAlgorithmException {
		logger.info("action=loggedIn");

		long guestId = ControllerHelper.getGuestId();

		ModelAndView mav = new ModelAndView("redirect:main");
		String targetEnvironment = env.get("environment");
		mav.addObject("prod", targetEnvironment.equals("prod"));
		if (request.getSession(false) == null)
			return mav;
		
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth == null || !auth.isAuthenticated())
			return mav;
		mav.setViewName("log");

		if (request.getSession().getAttribute("homeModel") == null)
			throw new RuntimeException(
					"Could not determine timeZone. Are cookies enabled?");
		
		Guest guest = guestService.getGuestById(guestId);
		
		String verificationCode = Utils.sha1Hash("147543b0f2b72f9d00ff3a0d10c3ff28cb590dcc"+guest.email);
		mav.addObject("testerHash", verificationCode);
		String release = env.get("release");
		request.setAttribute("guestName", guest.getGuestName());
		if (SecurityUtils.isDemoUser())
			request.setAttribute("demo", true);
		if (release != null)
			mav.addObject("release", release);
		return mav;
	}

	private void initializeWithTimeZone(HttpServletRequest request, long guestId) {
		Cookie[] cookies = request.getCookies();
		String timeZone = null, date = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equalsIgnoreCase("timeZone")) {
				timeZone = cookie.getValue();

				HomeModel homeModel = beanFactory.getBean(HomeModel.class);
				homeModel.init(timeZone);
				request.getSession().setAttribute("homeModel", homeModel);
			} else if (cookie.getName().equalsIgnoreCase("date")) {
				date = cookie.getValue();
			}
		}

		metadataService.setTimeZone(guestId, date, timeZone);
	}
}
