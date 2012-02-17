package com.fluxtream.mvc.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.mvc.models.CommentModel;
import com.fluxtream.mvc.models.HomeModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.SecurityUtils;
import com.google.gson.Gson;

@Controller
public class DiaryController {

	Logger logger = Logger.getLogger(DiaryController.class);

	@Autowired
	ApiDataService apiDataService;

	@Autowired
	FacetsHelper statsHelper;

	@Autowired
	MetadataService metadataService;

	final Gson gson = new Gson();

	@RequestMapping(value = "/diary/set/title")
	public void setCommentTitle(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String commentTitle = request.getParameter("commentTitle");

		logger.info("action=setDiaryTitle");

		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = statsHelper.getHomeModel(request);
		long guestId = ControllerHelper.getGuestId();

		homeModel.setTitle(commentTitle);
		metadataService.setDayCommentTitle(guestId, homeModel.getDate(), commentTitle);

		response.getWriter().write(commentTitle);
	}

	@RequestMapping(value = "/diary/set/body")
	public void setComment(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String commentBody = request.getParameter("commentBody");
		logger.info("action=setDiaryBody");

		response.setContentType("application/json; charset=utf-8");
		HomeModel homeModel = statsHelper.getHomeModel(request);
		long guestId = ControllerHelper.getGuestId();
		metadataService.setDayCommentBody(guestId, homeModel.getDate(), commentBody);
		response.getWriter().write(commentBody);
	}

	@RequestMapping(value = "/diary/get/title")
	public void getCommentCollapsed(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		HomeModel homeModel = statsHelper.getHomeModel(request);
		String title = homeModel.getTitle();
		if (title == null)
			title = "";
		String lines[] = title.split("[\\r\\n]+");
		response.setContentType("text/plain; charset=utf-8");
		if (SecurityUtils.isDemoUser())
			response.getWriter().write("***demo - log entry title hidden***");
		else
			response.getWriter().write(lines[0]);
	}

	@RequestMapping(value = "/diary/get/body")
	public void getCommentExpanded(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		HomeModel homeModel = statsHelper.getHomeModel(request);
		long guestId = ControllerHelper.getGuestId();
		String body = getDiaryBody(guestId, homeModel);
		response.setContentType("text/plain; charset=utf-8");
		if (SecurityUtils.isDemoUser())
			response.getWriter().write("***demo - log entry body hidden***");
		else
			response.getWriter().write(body);
	}

	@RequestMapping(value = "/diary/get/titleAndBody")
	public void getComment(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		HomeModel homeModel = statsHelper.getHomeModel(request);
		long guestId = ControllerHelper.getGuestId();
		DayMetadataFacet metadata = metadataService.getDayMetadata(guestId,
				homeModel.getDate(), true);
		String title = metadata.title;
		String body = metadata.comment;
		CommentModel comment = new CommentModel();
		if (SecurityUtils.isDemoUser()) {
			comment.title = "***demo - log entry title hidden***";
			comment.body = "***demo - log entry body hidden***";
		} else {
			comment.title = title==null?"":title;
			comment.body = body==null?"":body;
		}
		String json = gson.toJson(comment);
		response.setContentType("application/json; charset=utf-8");
		response.getWriter().write(json);
	}

	public String getDiaryTitle(long guestId, HomeModel homeModel) {
		DayMetadataFacet metadata = metadataService.getDayMetadata(guestId,
				homeModel.getDate(), true);
		return metadata.title==null?"":metadata.title;
	}

	public String getDiaryBody(long guestId, HomeModel homeModel) {
		DayMetadataFacet metadata = metadataService.getDayMetadata(guestId,
				homeModel.getDate(), true);
		return metadata.comment==null?"":metadata.comment;
	}

}