package com.fluxtream.connectors.controllers;

import javax.servlet.http.HttpServletRequest;

public class ControllerSupport {

	public static String error(HttpServletRequest request, String errorMessage, String stackTrace) {
		request.setAttribute("errorMessage", errorMessage);
		request.setAttribute("stackTrace", stackTrace);
		return "error";
	}

}
