package com.fluxtream.mvc.controllers;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.views.ViewsHelper;
import com.fluxtream.services.GuestService;

@Controller
public class ViewsController {

	@Autowired
	GuestService guestService;

	@Autowired
	FacetsHelper statsHelper;
	
	@Autowired
	Configuration env;

	@RequestMapping("/views/dashboard")
	public ModelAndView getDashboard(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("views/dashboard");
		return mav;
	}

	@RequestMapping("/views/search")
	public ModelAndView getSearch(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("views/search");
		return mav;
	}

	@RequestMapping("/views/list")
	public ModelAndView getList(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		request.setAttribute("hasWeight", guestService.hasApiKey(
				ControllerHelper.getGuestId(),
				Connector.getConnector("withings")));
		request.setAttribute("hasFitbit", guestService.hasApiKey(
				ControllerHelper.getGuestId(),
				Connector.getConnector("fitbit")));
		long guestId = ControllerHelper.getGuestId();
		List<ApiKey> userKeys = guestService.getApiKeys (guestId);
		mav.addObject("userConnectors", userKeys);
		mav.addObject("userConnectorModels", ViewsHelper
				.toConnectorModels(userKeys));
		mav.setViewName("views/list");
		return mav;
	}

	@RequestMapping("/views/clock")
	public ModelAndView getClock(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		Guest guest = ControllerHelper.getGuest();
		if (guestService.getApiKeys(guest.getId()).size() == 0) {
			mav.setViewName("views/noConnectors");
		} else {
			mav.setViewName("views/clock");
			long guestId = ControllerHelper.getGuestId();
			List<ApiKey> userKeys = guestService.getApiKeys (guestId);
			mav.addObject("userConnectors", userKeys);
			mav.addObject("userConnectorModels", ViewsHelper
					.toConnectorModels(userKeys));
			request.setAttribute("release", env.get("release"));
			request.setAttribute("hasWeight", guestService.hasApiKey(
					ControllerHelper.getGuestId(),
					Connector.getConnector("withings")));
			request.setAttribute("hasFitbit", guestService.hasApiKey(
					ControllerHelper.getGuestId(),
					Connector.getConnector("fitbit")));
		}
		return mav;
	}

	@RequestMapping("/views/timeline")
	public ModelAndView getTimeline(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("views/timeline");
		request.setAttribute("hasWeight", guestService.hasApiKey(
				ControllerHelper.getGuestId(),
				Connector.getConnector("withings")));
		request.setAttribute("hasFitbit", guestService.hasApiKey(
				ControllerHelper.getGuestId(),
				Connector.getConnector("fitbit")));
		return mav;
	}

	@RequestMapping("/views/tools")
	public ModelAndView getTools(HttpServletRequest request,
			HttpServletResponse response) {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("views/tools");
		return mav;
	}
}
