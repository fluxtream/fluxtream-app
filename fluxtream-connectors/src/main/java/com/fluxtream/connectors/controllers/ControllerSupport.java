package com.fluxtream.connectors.controllers;

import javax.servlet.http.HttpServletRequest;

public class ControllerSupport {

	public static String error(HttpServletRequest request, String errorMessage, String stackTrace) {
		request.setAttribute("errorMessage", errorMessage);
		request.setAttribute("stackTrace", stackTrace);
		return "error";
	}

    public static final String getLocationBase(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String locationBase = String.format("%s://%s:%s/", scheme, serverName, serverPort);
        return locationBase;
    }

}
