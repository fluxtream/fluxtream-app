package com.fluxtream.mvc.tabs.controllers;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.services.ApiDataService;

@Controller
public class DashboardTabController {

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	@Qualifier("dayWidgetsStatsHelper")
	DashboardWidgetsHelper dayStatsHelper;

	@Autowired
	@Qualifier("weekWidgetsStatsHelper")
	DashboardWidgetsHelper weekStatsHelper;

	@RequestMapping("/tabs/dashboard")
	public ModelAndView getUserWidgets(HttpServletRequest request) {
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		long guestId = ControllerHelper.getGuestId();
		ModelAndView mav = new ModelAndView();
		mav.setViewName("tabs/stats");
		mav.addObject("timeUnit", homeModel.getTimeInterval().timeUnit
				.toString().toLowerCase());
		switch (homeModel.getTimeInterval().timeUnit) {
		case DAY:
			mav.addObject("userWidgets",
					dayStatsHelper.getAvailableUserWidgets(guestId));
			break;
		case WEEK:
			mav.addObject("userWidgets",
					weekStatsHelper.getAvailableUserWidgets(guestId));
			break;
		}
		return mav;
	}

	@RequestMapping("/tabs/dashboard.json")
	public void getDayStatsWidgetJson(HttpServletResponse response,
			HttpServletRequest request) throws IOException {
		HomeModel homeModel = ControllerHelper.getHomeModel(request);
		long guestId = ControllerHelper.getGuestId();
		JSONObject o = new JSONObject();
		switch (homeModel.getTimeInterval().timeUnit) {
		case DAY:
			List<DashboardWidgetsHelper.DashboardWidget> availableUserWidgets = dayStatsHelper
					.getAvailableUserWidgets(guestId);
			dayStatsHelper.provideWidgetsData(availableUserWidgets, guestId,
					homeModel.getTimeInterval(), o);
			break;
		case WEEK:
			availableUserWidgets = weekStatsHelper
					.getAvailableUserWidgets(guestId);
			weekStatsHelper.provideWidgetsData(availableUserWidgets, guestId,
					homeModel.getTimeInterval(), o);
			break;
		case MONTH:
			break;
		case YEAR:
			break;
		}
		response.setContentType("application/json");
		response.getWriter().write(o.toString());
	}

}
