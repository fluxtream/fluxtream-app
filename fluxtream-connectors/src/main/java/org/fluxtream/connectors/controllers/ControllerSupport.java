package org.fluxtream.connectors.controllers;

import javax.servlet.http.HttpServletRequest;
import org.fluxtream.core.Configuration;

public class ControllerSupport {

	public static String error(HttpServletRequest request, String errorMessage, String stackTrace) {
		request.setAttribute("errorMessage", errorMessage);
		request.setAttribute("stackTrace", stackTrace);
		return "error";
	}

    /**
     * WARNING: this method assumes that if there is an x-forwared-host header (as is the case
     * when using the ProxyPass/ProxyReverse directive with apache, then whatever port this query
     * was made with gets stripped off
     * @param request used to figure out our server name and wether this request is forwarded from apache
     * @param env used to find out wether https needs to be enforced on the returned url
     * @return the base url for this server
     */
    public static final String getLocationBase(HttpServletRequest request, Configuration env) {
        return env.get("homeBaseUrl");
        //String scheme = request.getScheme();
        //String serverName = request.getServerName();
        //String forwardedHost = request.getHeader("x-forwarded-host");
        //if (forwardedHost!=null) {
        //    boolean forceHttps = env.get("forceHttps")!=null && env.get("forceHttps").equalsIgnoreCase("true");
        //    if (forceHttps) {
        //        String locationBase = new StringBuilder("https://").append(forwardedHost).append("/").toString();
        //        return locationBase;
        //    } else {
        //        String locationBase = String.format("%s://%s/", scheme, forwardedHost);
        //        return locationBase;
        //    }
        //} else {
        //    int serverPort = request.getServerPort();
        //    String locationBase = String.format("%s://%s:%s/", scheme, serverName, serverPort);
        //    return locationBase;
        //}
    }

}
