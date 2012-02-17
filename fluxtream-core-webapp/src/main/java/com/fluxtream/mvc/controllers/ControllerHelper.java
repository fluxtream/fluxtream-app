package com.fluxtream.mvc.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.HomeModel;

public class ControllerHelper {
	
	public static long getGuestId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		long guestId = ((FlxUserDetails)auth.getPrincipal()).getGuest().getId();
		return guestId;
	}
	
	public static String getGuestConnectorConfigStateKey() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String key = ((FlxUserDetails)auth.getPrincipal()).getGuest().connectorConfigStateKey;
		return key;
	}
	
	public static Guest getGuest() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Guest guest = ((FlxUserDetails)auth.getPrincipal()).getGuest();
		return guest;
	}

	public static HomeModel getHomeModel(HttpServletRequest request) {
		HomeModel homeModel = (HomeModel) request.getSession().getAttribute("homeModel");
		return homeModel;
	}

}
