package com.fluxtream.mvc.controllers;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fluxtream.Configuration;
import com.fluxtream.utils.HttpUtils;

@Controller
@RequestMapping("/bodytrack")
public class BodyTrackController {
	Logger logger = Logger.getLogger(BodyTrackController.class);

	@Autowired
	Configuration env;

	@RequestMapping(value = "/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
	public void index(HttpServletResponse response,
			@PathVariable("UID") String uid,
			@PathVariable("DeviceNickname") String deviceNickname,
			@PathVariable("ChannelName") String channelName,
			@PathVariable("Level") String level,
			@PathVariable("Offset") String offset) throws HttpException, IOException {
		String bodyTrackUrl = "http://localhost:3000/tiles/" + uid + "/"
				+ deviceNickname + "." + channelName + "/" + level + "."
				+ offset + ".json";
		writeTunnelResponse(bodyTrackUrl, response);
	}
	
	@RequestMapping(value = "/users/{UID}/views")
	public void bodyTrackViews(HttpServletResponse response,
			@PathVariable("UID") String UID) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/views";
		writeTunnelResponse(tunnelUrl, response);
	}
	
	@RequestMapping(value = "/users/{UID}/views/get")
	public void bodyTrackView(HttpServletResponse response,
			@PathVariable("UID") String UID, @RequestParam("name") String name) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/views/get?name=" + name;
		writeTunnelResponse(tunnelUrl, response);
	}
	
	private void writeTunnelResponse(String tunnelUrl, HttpServletResponse response) throws HttpException, IOException {
		String contents = HttpUtils.fetch(tunnelUrl, env);
		response.getWriter().write(contents);
	}

}
