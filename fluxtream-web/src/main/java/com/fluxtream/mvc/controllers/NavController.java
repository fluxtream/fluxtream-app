package com.fluxtream.mvc.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fluxtream.TimeUnit;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fluxtream.Configuration;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.mvc.models.VisualizationType;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;

@Controller
public class NavController {

	Logger logger = Logger.getLogger(NavController.class);

	@Autowired
	GuestService guestService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	Configuration env;

	@RequestMapping(value = "/nav/model.json")
	public void getModel(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		updateComment(homeModel, request);
		response.getWriter().write(
				homeModel.toJSONString(env, getConfigState(request)));
	}

	private String getConfigState(HttpServletRequest request) {
		return ControllerHelper.getGuestConnectorConfigStateKey();
	}

	private void updateComment(HomeModel homeModel, HttpServletRequest request) {
		long guestId = ControllerHelper.getGuestId();
		DayMetadataFacet metadata = metadataService.getDayMetadata(guestId, homeModel.getDate(), true);
		homeModel.setTitle(metadata.title!=null?metadata.title:"");
	}

	@RequestMapping(value = "/nav/setVisualizationType.json")
	public void setVisualizationType(
			@RequestParam("visualizationType") String value,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.info("action=change_visualization value=" + value);
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setViewType(VisualizationType.fromValue(value));
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setToToday.json")
	public void setToToday(HttpServletRequest request,
			HttpServletResponse response,  @RequestParam("timeUnit") String timeUnit) throws IOException {
		logger.info("action=setToToday");
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setToToday(TimeUnit.fromValue(timeUnit));
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/date/{date}")
	public void gotoDate(@PathVariable("date") String date,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		setDate(date, request, response);
	}

	@RequestMapping(value = "/nav/setWeek.json")
	public void setWeek(@RequestParam("year") int year,
			@RequestParam("week") int week,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.info("action=setWeek year=" + year + " week=" + week);
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setWeek(year, week);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setMonth.json")
	public void setMonth(@RequestParam("year") int year,
			@RequestParam("month") int month,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.info("action=setMonth year=" + year + " month=" + month);
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setMonth(year, month);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setYear.json")
	public void setYear(@RequestParam("year") int year,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.info("action=setYear year=" + year);
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setYear(year);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setDate.json")
	public void setDate(@RequestParam("date") String date,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.info("action=setDate date=" + date);
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setDate(date);
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/decrementTimespan.json")
	public void decrementTimespan(HttpServletRequest request,
			HttpServletResponse response,
            @RequestParam("state") String state) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.decrementTimespan(state);
		logger.info("action=decrementTimespan");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/incrementTimespan.json")
	public void incrementTimespan(HttpServletRequest request,
			HttpServletResponse response,
            @RequestParam("state") String state) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.incrementTimespan(state);
		logger.info("action=incrementTimespan");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setDayTimeUnit.json")
	public void setDayTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		logger.info("action=setDayTimeUnit");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setDayTimeUnit();
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setWeekTimeUnit.json")
	public void setWeekTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setWeekTimeUnit();
		logger.info("action=setWeekTimeUnit");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setMonthTimeUnit.json")
	public void setMonthTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setMonthTimeUnit();
		logger.info("action=setMonthTimeUnit");
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setYearTimeUnit.json")
	public void setYearTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setYearTimeUnit();
		logger.info("action=setYearTimeUnit");
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/nav/setContinuousTimeUnit.json")
	public void setContinuousTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		logger.info("action=setContinuousTimeUnit");
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}
	
}
