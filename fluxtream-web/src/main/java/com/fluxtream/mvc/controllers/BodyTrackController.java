package com.fluxtream.mvc.controllers;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;

@Controller
public class BodyTrackController {
	Logger logger = Logger.getLogger(AppController.class);

	@Autowired
	Configuration env;

	@RequestMapping(value = "/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
	public ModelAndView index(HttpServletRequest request,
			@PathVariable("UID") String uid,
			@PathVariable("DeviceNickname") String deviceNickname,
			@PathVariable("ChannelName") String channelName,
			@PathVariable("Level") String level,
			@PathVariable("Offset") String offset) throws MalformedURLException {
		String bodyTrackUrl = "http://bodytrack.org/tiles/" + uid + "/"
				+ deviceNickname + "." + channelName + "/" + level + "."
				+ offset + ".json";
		URL url = new URL(bodyTrackUrl);
		// etc, etc...
		return null;
	}

}
