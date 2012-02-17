package com.fluxtream.mvc.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

	@RequestMapping(value = "/home/model.json")
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

	@RequestMapping(value = "/home/setVisualizationType.json")
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

	@RequestMapping(value = "/home/setToToday.json")
	public void setToToday(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		logger.info("action=setToToday");
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setToToday();
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/home/setDate.json")
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

	@RequestMapping(value = "/home/decrementTimespan.json")
	public void decrementTimespan(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.decrementTimespan();
		logger.info("action=decrementTimespan");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/home/incrementTimespan.json")
	public void incrementTimespan(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.incrementTimespan();
		logger.info("action=incrementTimespan");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/home/setDayTimeUnit.json")
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

	@RequestMapping(value = "/home/setWeekTimeUnit.json")
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

	@RequestMapping(value = "/home/setMonthTimeUnit.json")
	public void setMonthTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setMonthTimeUnit();
		logger.info("action=setMonthTimeUnit");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}

	@RequestMapping(value = "/home/setYearTimeUnit.json")
	public void setYearTimeUnit(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute(
				"homeModel");
		homeModel.setYearTimeUnit();
		logger.info("action=setYearTimeUnit");
		updateComment(homeModel, request);
		response.getWriter().write(homeModel.toJSONString(env, getConfigState(request)));
	}
	
}
