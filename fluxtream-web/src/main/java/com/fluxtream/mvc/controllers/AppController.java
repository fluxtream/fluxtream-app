package com.fluxtream.mvc.controllers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

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

@Controller
public class AppController {

	Logger logger = Logger.getLogger(AppController.class);

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

	@RequestMapping(value = { "main", "", "/", "/welcome" })
	public ModelAndView index(HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth != null && auth.isAuthenticated())
			return new ModelAndView("redirect:/home");
		ModelAndView mav = new ModelAndView("index");
		String release = env.get("release");
		if (release != null)
			mav.addObject("release", release);
		String targetEnvironment = env.get("environment");
		mav.addObject("prod", targetEnvironment.equals("prod"));
		return mav;
	}

	public ModelAndView home(HttpServletRequest request) {
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
		mav.setViewName("home");

		Guest guest = guestService.getGuestById(guestId);
		
		mav.addObject("fullname", guest.getGuestName());

		String release = env.get("release");
		request.setAttribute("guestName", guest.getGuestName());
		if (SecurityUtils.isDemoUser())
			request.setAttribute("demo", true);
		if (release != null)
			mav.addObject("release", release);
		return mav;
	}

	@RequestMapping(value = "/home")
	public ModelAndView welcomeHome(HttpServletRequest request)
			throws IOException, NoSuchAlgorithmException {
		long guestId = ControllerHelper.getGuestId();
		checkIn(request, guestId);
		return home(request);
	}

	@RequestMapping(value = "/home/date/{date}")
	public ModelAndView date(@PathVariable("date") String date,
			HttpServletRequest request) throws IOException,
			NoSuchAlgorithmException {

		long guestId = ControllerHelper.getGuestId();
		if (request.getSession().getAttribute("homeModel")==null) {
			checkIn(request, guestId);
		}
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setDate(date);

		return home(request);
	}

	private void checkIn(HttpServletRequest request, long guestId)
			throws IOException {
		String remoteAddr = request.getHeader("X-Forwarded-For");
		if (remoteAddr == null)
			remoteAddr = request.getRemoteAddr();
		guestService.checkIn(guestId, remoteAddr);
		initializeWithTimeZone(request, guestId);
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
