package com.fluxtream.mvc.controllers;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.CalendarModel;

public class ControllerHelper {
	
	public static long getGuestId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		long guestId = ((FlxUserDetails)auth.getPrincipal()).getGuest().getId();
		return guestId;
	}
	
	public static Guest getGuest() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth==null)
			return null;
		Guest guest = ((FlxUserDetails)auth.getPrincipal()).getGuest();
		return guest;
	}

	public static CalendarModel getHomeModel(HttpServletRequest request) {
		CalendarModel calendarModel = (CalendarModel) request.getSession().getAttribute("calendarModel");
		return calendarModel;
	}

}
