package com.fluxtream.mvc.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.NotificationsService;
import com.google.gson.Gson;

@Controller
public class NotificationsController {

	Logger logger = Logger.getLogger(NotificationsController.class);

	@Autowired
	NotificationsService notificationsService;

	private final Gson gson = new Gson();

	@RequestMapping("/notifications/discard")
	public void discardNotifications(@RequestParam("ids") String ids,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		long guestId = ControllerHelper.getGuestId();

		String[] idStrings = ids.split(",");
		for (String idString : idStrings) {
			long id = Long.valueOf(idString);
			notificationsService.deleteNotification(guestId, id);
		}
		
		logger.info("action=discardNotifications");

		StatusModel status = new StatusModel(true, "notifications deleted");
		String json = gson.toJson(status);

		response.getWriter().write(json);
	}

}
