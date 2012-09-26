package com.fluxtream.mvc.controllers;

import javax.servlet.http.HttpServletRequest;

import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.SharedConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.models.CalendarModel;

public class AuthHelper {
	
	public static long getGuestId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		long guestId = ((FlxUserDetails)auth.getPrincipal()).getGuest().getId();
		return guestId;
	}

    public static boolean isViewingGranted(String connectorName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.coachee==null)
            return true;
        else {
            for (SharedConnector sharedConnector : principal.coachee.sharedConnectors) {
                if (sharedConnector.connectorName.equals(connectorName))
                    return true;
            }
            return false;
        }
    }

    public static void as(CoachingBuddy coachee) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        principal.coachee = coachee;
    }

    public static long getVieweeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        if (principal.coachee==null)
            return principal.getGuest().getId();
        else
            return principal.coachee.guestId;
    }

    public static CoachingBuddy getCoachee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final FlxUserDetails principal = (FlxUserDetails) auth.getPrincipal();
        return principal.coachee;
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
