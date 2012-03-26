package com.fluxtream.mvc.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
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

// This controller tunnels BodyTrack API calls to a BodyTrack server running on localhost:3000
// For documentation on these APIs, see 
//  https://fluxtream.atlassian.net/wiki/display/FLX/BodyTrack+server+APIs

@Controller
@RequestMapping("/bodytrack")
public class BodyTrackController {
	Logger logger = Logger.getLogger(BodyTrackController.class);

	@Autowired
	Configuration env;

	@RequestMapping(value = "/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
	public void bodyTrackTileFetch(HttpServletResponse response,
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
	
	@RequestMapping(value = "/photos/{UID}/{Level}.{Offset}.json")
	public void bodyTrackPhotoTileFetch(HttpServletResponse response,
			HttpServletRequest request,
			@PathVariable("UID") String uid,
			@PathVariable("Level") String level,
			@PathVariable("Offset") String offset) throws HttpException, IOException {
		String bodyTrackUrl = "http://localhost:3000/photos/" + uid + "/" + level + "."
				+ offset + ".json";
		String pstr = request.getQueryString();
		if(pstr != null) {
				bodyTrackUrl += "?" + pstr;
		}
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
	
	@RequestMapping(value = "/users/{UID}/views/set")
	public void bodyTrackSetView(HttpServletResponse response,
			@PathVariable("UID") String UID,
			@RequestParam("name") String name, @RequestParam("data") String data) throws HttpException, IOException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("name", name);
		params.put("data", data);
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/views/set";
		postTunnelRequest(tunnelUrl, response, params);
	}

	@RequestMapping(value = "/users/{UID}/sources")
	public void bodyTrackSources(HttpServletResponse response,
			@PathVariable("UID") String UID) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/sources";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/users/{UID}/sources/list")
	public void bodyTrackSourcesList(HttpServletResponse response,
			@PathVariable("UID") String UID) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/sources/list";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/users/{UID}/sources/default_graph_specs")
	public void bodyTrackGetDefaultGraphSpecs(HttpServletResponse response,
			@PathVariable("UID") String UID, @RequestParam("name") String name) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/sources/default_graph_specs?name=" + name;
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/users/{UID}/tags")
	public void bodyTrackTags(HttpServletResponse response,
			@PathVariable("UID") String UID) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/tags";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/users/{UID}/tags/{LOGREC_ID}/get")
	public void bodyTrackGetTags(HttpServletResponse response,
			@PathVariable("UID") String UID,
			@PathVariable("LOGREC_ID") String LOGREC_ID) throws HttpException, IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/tags/" + LOGREC_ID + "/get";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/users/{UID}/tags/{LOGREC_ID}/set")
	public void bodyTrackSetTags(HttpServletResponse response,
			@PathVariable("UID") String UID,
			@PathVariable("LOGREC_ID") String LOGREC_UID,
			@RequestParam("tags") String tags) throws HttpException, IOException {
		Map<String,String> params = new HashMap<String,String>();
		params.put("tags", tags);
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/tags/" + LOGREC_ID + "/set";
		postTunnelRequest(tunnelUrl, response, params);
	}

	@RequestMapping(value = "/users/{UID}/channels/{DeviceNickname}.{ChannelName}/set")
	public void bodyTrackChannelSet(HttpServletResponse response,
			@PathVariable("UID") String uid,
			@PathVariable("DeviceNickname") String deviceNickname,
			@PathVariable("ChannelName") String channelName,
			@RequestParam("user_default_style") String style) throws HttpException, IOException {
		String bodyTrackUrl = "http://localhost:3000/users/" + uid + "/channels/"
				+ deviceNickname + "." + channelName + "/set?user_default_style=" + URLEncoder.encode(style,"utf-8");
		writeTunnelResponse(bodyTrackUrl, response);
	}

	private void writeTunnelResponse(String tunnelUrl, HttpServletResponse response) throws HttpException, IOException {
		String contents = HttpUtils.fetch(tunnelUrl, env);
		response.getWriter().write(contents);
	}

	private void postTunnelRequest(String tunnelUrl, HttpServletResponse response, Map<String,String> params) throws HttpException, IOException {
		String contents = HttpUtils.fetch(tunnelUrl, params, env);
		response.getWriter().write(contents);
	}

}
