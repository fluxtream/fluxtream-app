package com.fluxtream.mvc.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fluxtream.Configuration;
import com.fluxtream.connectors.Connector;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.utils.HttpUtils;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// This controller tunnels BodyTrack API calls to a BodyTrack server running on localhost:3000
// For documentation on these APIs, see
//  https://fluxtream.atlassian.net/wiki/display/FLX/BodyTrack+server+APIs

@Controller
public class BodyTrackController {

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

    @Autowired
    BodyTrackHelper bodyTrackhelper;

	@RequestMapping(value = "/bodytrack/UID")
	public void bodyTrackUIDFetch(HttpServletResponse response)
            throws IOException {
		long guestId = ControllerHelper.getGuestId();
		String user_id = guestService.getApiKeyAttribute(guestId,
				Connector.getConnector("bodytrack"), "user_id");
		JSONObject json = new JSONObject();
		json.accumulate("user_id", user_id);
		response.getWriter().write(json.toString());
	}

	@RequestMapping(value = "/bodytrack/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
	public void bodyTrackTileFetch(HttpServletResponse response,
			@PathVariable("UID") String uid,
			@PathVariable("DeviceNickname") String deviceNickname,
			@PathVariable("ChannelName") String channelName,
			@PathVariable("Level") int level,
			@PathVariable("Offset") int offset) throws IOException {
        String result = bodyTrackhelper.fetchTile(uid,deviceNickname,channelName,level,offset);
		/*String bodyTrackUrl = "http://localhost:3000/tiles/" + uid + "/"
				+ deviceNickname + "." + channelName + "/" + level + "."
				+ offset + ".json";*/
        response.getWriter().write(result);
		//writeTunnelResponse(bodyTrackUrl, response);
	}

    @RequestMapping(value = "/bodytrack/users/{UID}/log_items/get")
    public void bodyTrackLogItemsGet(HttpServletResponse response,
                                     HttpServletRequest request,
                                     @PathVariable("UID") String uid) throws IOException {
        String bodyTrackUrl = "http://localhost:3000/users/" + uid + "/log_items/get";
        String pstr = request.getQueryString();
        if (pstr != null) {
            bodyTrackUrl += "?" + pstr;
        }
        writeTunnelResponse(bodyTrackUrl, response);
    }

    @RequestMapping(value = "/bodytrack/photos/{UID}/{Level}.{Offset}.json")
	public void bodyTrackPhotoTileFetch(HttpServletResponse response,
			HttpServletRequest request, @PathVariable("UID") String uid,
			@PathVariable("Level") String level,
			@PathVariable("Offset") String offset) throws HttpException,
			IOException {
		String bodyTrackUrl = "http://localhost:3000/photos/" + uid + "/"
				+ level + "." + offset + ".json";
		String pstr = request.getQueryString();
		if (pstr != null) {
			bodyTrackUrl += "?" + pstr;
		}
		writeTunnelResponse(bodyTrackUrl, response);
	}

    // NOTE: This one has request mappings both with and without "/bodytrack" because the
    // GWT layer currently has no way to customize the URLs
    @RequestMapping(value = {"/users/{UID}/logphotos/{PhotoSpec}.jpg",
                             "/bodytrack/users/{UID}/logphotos/{PhotoSpec}.jpg"})
    public void bodyTrackLogPhotosFetch(HttpServletResponse response,
                                        HttpServletRequest request,
                                        @PathVariable("UID") String uid,
                                        @PathVariable("PhotoSpec") String photoSpec) throws IOException {
        String bodyTrackUrl = "http://localhost:3000/users/" + uid + "/logphotos/" + photoSpec + ".jpg";
        String pstr = request.getQueryString();
        if (pstr != null) {
            bodyTrackUrl += "?" + pstr;
        }
        final byte[] contents = HttpUtils.fetchBinary(bodyTrackUrl, env);
        response.setContentLength(contents == null ? 0 : contents.length);
        if (contents != null) {
            final ByteArrayInputStream in = new ByteArrayInputStream(contents);
            final ServletOutputStream out = response.getOutputStream();
            if (out != null) {
                IOUtils.copy(in, out);
            }
        }
    }

    @RequestMapping(value = "/bodytrack/users/{UID}/views")
	public void bodyTrackViews(HttpServletResponse response,
			@PathVariable("UID") String UID) throws IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/views";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/views/get")
	public void bodyTrackView(HttpServletResponse response,
			@PathVariable("UID") String UID, @RequestParam("id") String id)
            throws IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID
				+ "/views/get?id=" + id;
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/views/set")
	public void bodyTrackSetView(HttpServletResponse response,
			@PathVariable("UID") String UID, @RequestParam("name") String name,
			@RequestParam("data") String data) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("name", name);
		params.put("data", data);
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/views/set";
		postTunnelRequest(tunnelUrl, response, params);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/sources")
	public void bodyTrackSources(HttpServletResponse response,
			@PathVariable("UID") String UID) throws IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/sources";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/sources/list")
	public void bodyTrackSourcesList(HttpServletResponse response,
			@PathVariable("UID") String UID) throws IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID
				+ "/sources/list";
        response.getWriter().write(bodyTrackhelper.listSources(UID));
		//writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/sources/default_graph_specs")
	public void bodyTrackGetDefaultGraphSpecs(HttpServletResponse response,
			@PathVariable("UID") String UID, @RequestParam("name") String name)
            throws IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID
				+ "/sources/default_graph_specs?name=" + name;
		writeTunnelResponse(tunnelUrl, response);
	}

    @RequestMapping(value = "/bodytrack/users/{UID}/logrecs/{LOGREC_ID}/get")
    public void bodyTrackGetMetadata(HttpServletResponse response,
    			HttpServletRequest request, @PathVariable("UID") String UID,
    			@PathVariable("LOGREC_ID") String LOGREC_ID) throws IOException {
    	String bodyTrackUrl = "http://localhost:3000/users/" + UID + "/logrecs/" + LOGREC_ID + "/get";
    	String pstr = request.getQueryString();
    	if (pstr != null) {
    		bodyTrackUrl += "?" + pstr;
    	}
    	writeTunnelResponse(bodyTrackUrl, response);
    }

    @RequestMapping(value = "/bodytrack/users/{UID}/logrecs/{LOGREC_ID}/set")
    public void bodyTrackSetMetadata(HttpServletResponse response,
        		HttpServletRequest request, @PathVariable("UID") String UID,
        		@PathVariable("LOGREC_ID") String LOGREC_ID) throws Exception {
        // Here is the URL we need to proxy to.  This is a post so we need to copy
        // params one by one.  The API is documented at:
        //   https://fluxtream.atlassian.net/wiki/display/FLX/BodyTrack+server+APIs#BodyTrackserverAPIs-usersUIDlogrecsLOGRECIDset
        // The params are all optional:
        //   comment=<string> Set the comment field to the provided string
        //   tags=<list of tags separated by commas> Set the tags for the logrec.  Behaves the same as /users/UID/tags/LOGREC_ID/set?tags=<value> other than having a different return value.
        //    nsfw=<value> If specified, alters the value of the NSFW flag on the logrec and modifies tag list appropriately to either include an "nsfw" tag if value is true, or remove any existing "nsfw" tags if value is false.
        String bodyTrackUrl = "http://localhost:3000/users/" + UID + "/logrecs/" + LOGREC_ID + "/set";
        Map parameterMap = request.getParameterMap();
        Enumeration parameterNames = request.getParameterNames();
        Map<String,String> tunneledParameters = new HashMap<String,String>();
        while(parameterNames.hasMoreElements()) {
            String parameterName = (String)parameterNames.nextElement();
            String parameter = request.getParameter(parameterName);
            tunneledParameters.put(parameterName, parameter);
        }
        postTunnelRequest(bodyTrackUrl, response, tunneledParameters);
     }

	@RequestMapping(value = "/bodytrack/users/{UID}/tags")
	public void bodyTrackTags(HttpServletResponse response,
			@PathVariable("UID") String UID) throws IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/tags";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/tags/{LOGREC_ID}/get")
	public void bodyTrackGetTags(HttpServletResponse response,
			@PathVariable("UID") String UID,
			@PathVariable("LOGREC_ID") String LOGREC_ID) throws HttpException,
			IOException {
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/tags/"
				+ LOGREC_ID + "/get";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/tags/{LOGREC_ID}/set")
	public void bodyTrackSetTags(HttpServletResponse response,
			@PathVariable("UID") String UID,
			@PathVariable("LOGREC_ID") String LOGREC_ID,
			@RequestParam("tags") String tags) throws HttpException,
			IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("tags", tags);
		String tunnelUrl = "http://localhost:3000/users/" + UID + "/tags/"
				+ LOGREC_ID + "/set";
		postTunnelRequest(tunnelUrl, response, params);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/channels/{DeviceNickname}.{ChannelName}/set")
	public void bodyTrackChannelSet(HttpServletResponse response,
			@PathVariable("UID") String uid,
			@PathVariable("DeviceNickname") String deviceNickname,
			@PathVariable("ChannelName") String channelName,
			@RequestParam("user_default_style") String style)
            throws IOException {
		String bodyTrackUrl = "http://localhost:3000/users/" + uid
				+ "/channels/" + deviceNickname + "." + channelName
				+ "/set?user_default_style="
				+ URLEncoder.encode(style, "utf-8");
		writeTunnelResponse(bodyTrackUrl, response);
	}

	private void writeTunnelResponse(String tunnelUrl,
			HttpServletResponse response) throws IOException {
		String contents = HttpUtils.fetch(tunnelUrl, env);
		response.getWriter().write(contents);
	}

	private void postTunnelRequest(String tunnelUrl,
			HttpServletResponse response, Map<String, String> params)
            throws IOException {
		String contents = HttpUtils.fetch(tunnelUrl, params, env);
		response.getWriter().write(contents);
	}

}
