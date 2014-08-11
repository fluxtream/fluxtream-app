package org.fluxtream.mvc.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
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
@Deprecated
public class BodyTrackController {

	@Autowired
	Configuration env;

	@Autowired
	GuestService guestService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @RequestMapping(value = "/bodytrack/users/{UID}/log_items/get")
    public void bodyTrackLogItemsGet(HttpServletResponse response,
                                     HttpServletRequest request,
                                     @PathVariable("UID") Long uid) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        // TODO: this is really going to be problematic...
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
        String bodyTrackUrl = "http://localhost:3000/users/" + user_id + "/log_items/get";
        String pstr = request.getQueryString();
        if (pstr != null) {
            bodyTrackUrl += "?" + pstr;
        }
        writeTunnelResponse(bodyTrackUrl, response);
    }

    @RequestMapping(value = "/bodytrack/photos/{UID}/{Level}.{Offset}.json")
	public void bodyTrackPhotoTileFetch(HttpServletResponse response,
			HttpServletRequest request, @PathVariable("UID") Long uid,
			@PathVariable("Level") String level,
			@PathVariable("Offset") String offset) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        //TODO: WARNING!
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
		String bodyTrackUrl = "http://localhost:3000/photos/" + user_id + "/"
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
                                        @PathVariable("UID") Long uid,
                                        @PathVariable("PhotoSpec") String photoSpec) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        //TODO: WARNING!
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
        String bodyTrackUrl = "http://localhost:3000/users/" + user_id + "/logphotos/" + photoSpec + ".jpg";
        String pstr = request.getQueryString();
        if (pstr != null) {
            bodyTrackUrl += "?" + pstr;
        }
        final byte[] contents = HttpUtils.fetchBinary(bodyTrackUrl);
        response.setContentLength(contents == null ? 0 : contents.length);
        if (contents != null) {
            final ByteArrayInputStream in = new ByteArrayInputStream(contents);
            final ServletOutputStream out = response.getOutputStream();
            if (out != null) {
                IOUtils.copy(in, out);
            }
        }
    }

	@RequestMapping(value = "/bodytrack/users/{UID}/sources")
	public void bodyTrackSources(HttpServletResponse response,
			@PathVariable("UID") Long uid) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
		String tunnelUrl = "http://localhost:3000/users/" + user_id + "/sources";
		writeTunnelResponse(tunnelUrl, response);
	}

    @RequestMapping(value = "/bodytrack/users/{UID}/logrecs/{LOGREC_ID}/get")
    public void bodyTrackGetMetadata(HttpServletResponse response,
    			HttpServletRequest request, @PathVariable("UID") Long uid,
    			@PathVariable("LOGREC_ID") String LOGREC_ID) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
    	String bodyTrackUrl = "http://localhost:3000/users/" + user_id + "/logrecs/" + LOGREC_ID + "/get";
    	String pstr = request.getQueryString();
    	if (pstr != null) {
    		bodyTrackUrl += "?" + pstr;
    	}
    	writeTunnelResponse(bodyTrackUrl, response);
    }

    @RequestMapping(value = "/bodytrack/users/{UID}/logrecs/{LOGREC_ID}/set")
    public void bodyTrackSetMetadata(HttpServletResponse response,
        		HttpServletRequest request, @PathVariable("UID") Long uid,
        		@PathVariable("LOGREC_ID") String LOGREC_ID) throws Exception {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        // Here is the URL we need to proxy to.  This is a post so we need to copy
        // params one by one.  The API is documented at:
        //   https://fluxtream.atlassian.net/wiki/display/FLX/BodyTrack+server+APIs#BodyTrackserverAPIs-usersUIDlogrecsLOGRECIDset
        // The params are all optional:
        //   comment=<string> Set the comment field to the provided string
        //   tags=<list of tags separated by commas> Set the tags for the logrec.  Behaves the same as /users/UID/tags/LOGREC_ID/set?tags=<value> other than having a different return value.
        //    nsfw=<value> If specified, alters the value of the NSFW flag on the logrec and modifies tag list appropriately to either include an "nsfw" tag if value is true, or remove any existing "nsfw" tags if value is false.
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
        String bodyTrackUrl = "http://localhost:3000/users/" + user_id + "/logrecs/" + LOGREC_ID + "/set";
        Enumeration parameterNames = request.getParameterNames();
        Map<String,String> tunneledParameters = new HashMap<String,String>();
        while(parameterNames.hasMoreElements()) {
            String parameterName = (String)parameterNames.nextElement();
            String parameter = request.getParameter(parameterName);
            tunneledParameters.put(parameterName, parameter);
        }
        postTunnelRequest(bodyTrackUrl, response, tunneledParameters);
     }

    /**
     * @deprecated Use {@link org.fluxtream.api.BodyTrackController#getAllTagsForUser} instead.
     */
	@RequestMapping(value = "/bodytrack/users/{UID}/tags")
	public void bodyTrackTags(HttpServletResponse response,
			@PathVariable("UID") Long uid) throws IOException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        response.getWriter().write("[]");
        /*String user_id = guestService.getApiKeyAttribute(uid,Connector.getConnector("bodytrack"), "user_id");
		String tunnelUrl = "http://localhost:3000/users/" + user_id + "/tags";
		writeTunnelResponse(tunnelUrl, response);*/
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/tags/{LOGREC_ID}/get")
	public void bodyTrackGetTags(HttpServletResponse response,
			@PathVariable("UID") Long uid,
			@PathVariable("LOGREC_ID") String LOGREC_ID) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
		String tunnelUrl = "http://localhost:3000/users/" + user_id + "/tags/"
				+ LOGREC_ID + "/get";
		writeTunnelResponse(tunnelUrl, response);
	}

	@RequestMapping(value = "/bodytrack/users/{UID}/tags/{LOGREC_ID}/set")
	public void bodyTrackSetTags(HttpServletResponse response,
			@PathVariable("UID") Long uid,
			@PathVariable("LOGREC_ID") String LOGREC_ID,
			@RequestParam("tags") String tags) throws IOException, UnexpectedHttpResponseCodeException {
        if (!checkForPermissionAccess(uid)){
            uid = null;
        }
        ApiKey apiKey = guestService.getApiKey(uid, Connector.getConnector("bodytrack"));
        String user_id = guestService.getApiKeyAttribute(apiKey, "user_id");
		Map<String, String> params = new HashMap<String, String>();
		params.put("tags", tags);
		String tunnelUrl = "http://localhost:3000/users/" + user_id + "/tags/"
				+ LOGREC_ID + "/set";
		postTunnelRequest(tunnelUrl, response, params);
	}

	private void writeTunnelResponse(String tunnelUrl,
			HttpServletResponse response) throws IOException, UnexpectedHttpResponseCodeException {
        System.out.println("tunneled URL: " + tunnelUrl);
		String contents = HttpUtils.fetch(tunnelUrl);
		response.getWriter().write(contents);
	}

	private void postTunnelRequest(String tunnelUrl,
			HttpServletResponse response, Map<String, String> params) throws IOException, UnexpectedHttpResponseCodeException {
        System.out.println("tunneled URL: " + tunnelUrl);
		String contents = HttpUtils.fetch(tunnelUrl, params);
		response.getWriter().write(contents);
	}

    private boolean checkForPermissionAccess(long targetUid){
        Guest guest = AuthHelper.getGuest();
        return targetUid == guest.getId() || guest.hasRole(Guest.ROLE_ADMIN) || guest.hasRole(Guest.ROLE_ADMIN);
    }

}
