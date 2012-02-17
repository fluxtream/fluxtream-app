package com.fluxtream.mvc.controllers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification.Type;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.NotificationsService;
import com.fluxtream.utils.SecurityUtils;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;

@Controller
public class MainController {

	static Logger logger = Logger.getLogger(MainController.class);

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

	@RequestMapping(value = "/eraseEverything")
	public void eraseEverything(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Guest guest = ControllerHelper.getGuest();
		guestService.eraseGuestInfo(guest.username);
		StatusModel status = new StatusModel(true, "bye");
		String json = gson.toJson(status);
		response.setContentType("application/json; charset=utf-8");
		response.getWriter().write(json);
	}

	@RequestMapping(value = "/ping")
	public void ping(HttpServletResponse response) throws IOException {
		StatusModel status = new StatusModel(true, "alive");
		String json = gson.toJson(status);
		response.setContentType("application/json; charset=utf-8");
		response.getWriter().write(json);
	}

	@RequestMapping(value = "/notSupported")
	public ModelAndView notSupported(HttpServletRequest request) {
		logger.warn("action=notSupported");
		ModelAndView mav = new ModelAndView("notSupported");
		mav.addObject("release", env.get("release"));
		return mav;
	}

	@RequestMapping(value = { "main", "", "/", "/welcome" })
	public ModelAndView index(HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth != null && auth.isAuthenticated())
			return new ModelAndView("redirect:/home");
		ModelAndView mav = new ModelAndView("welcome");
		String release = env.get("release");
		if (release != null)
			mav.addObject("release", release);
		String targetEnvironment = env.get("environment");
		mav.addObject("prod", targetEnvironment.equals("prod"));
		return mav;
	}

	@RequestMapping(value = "/home/date/{date}")
	public ModelAndView homeDate(HttpServletRequest request, Principal p,
			@PathVariable String date) throws IOException, NoSuchAlgorithmException {
		if (date == null || date.equals("null"))
			return null;
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		homeModel.setDate(date);
		return home(request, p);
	}

	@RequestMapping(value = "/home/month/{year}/{month}")
	public ModelAndView homeMonth(HttpServletRequest request, Principal p,
			@PathVariable int year, @PathVariable int month) throws IOException, NoSuchAlgorithmException {
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		homeModel.setMonth(year, month);
		return home(request, p);
	}

	@RequestMapping(value = "/home/year/{year}")
	public ModelAndView homeYear(HttpServletRequest request, Principal p,
			@PathVariable int year) throws IOException, NoSuchAlgorithmException {
		return home(request, p);
	}

	@RequestMapping(value = "/home/week/{year}/{week}")
	public ModelAndView homeWeek(HttpServletRequest request, Principal p,
			@PathVariable int year, @PathVariable int week) throws IOException, NoSuchAlgorithmException {
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		homeModel.setWeek(year, week);
		return home(request, p);
	}

	@RequestMapping(value = "/home/from/{connectorName}")
	public String home(HttpServletRequest request,
			@PathVariable("connectorName") String connectorName) {
		long guestId = ControllerHelper.getGuestId();
		String message = "You have successfully added a new connector: "
				+ Connector.getConnector(connectorName).prettyName()
				+". Your data is now being retrieved. "
				+ "It may take a little while until it becomes visible.";
		notificationsService.addNotification(guestId, Type.INFO, message);
		return "redirect:/home";
	}
	
	public ModelAndView home(HttpServletRequest request, Principal p)
			throws IOException, NoSuchAlgorithmException {
		logger.info("action=loggedIn");

		long guestId = ControllerHelper.getGuestId();

		if (guestService.getApiKeys(guestId).size()==1)
			notificationsService.addNotification(guestId,Type.INFO, 
					"Congratulations! You have added your first connector. " +
					"Now add more: go to the gear menu &rarr; Connectors");

		ModelAndView mav = new ModelAndView("redirect:main");
		String targetEnvironment = env.get("environment");
		mav.addObject("prod", targetEnvironment.equals("prod"));
		if (request.getSession(false) == null)
			return mav;
		
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth == null || !auth.isAuthenticated())
			return mav;
		mav.setViewName("main");

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

	@RequestMapping(value = "/home")
	public ModelAndView welcomeHome(HttpServletRequest request, Principal p)
			throws IOException, NoSuchAlgorithmException {

		long guestId = ControllerHelper.getGuestId();
		String remoteAddr = request.getHeader("X-Forwarded-For");
		if (remoteAddr == null)
			remoteAddr = request.getRemoteAddr();
		guestService.checkIn(guestId, remoteAddr);
		initializeWithTimeZone(request, guestId);
		
		return home(request, p);
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

	@RequestMapping(value = "/accessDenied")
	public String accessDenied() {
		return "accessDenied";
	}

}
