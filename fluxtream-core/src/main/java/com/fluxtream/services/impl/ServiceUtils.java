package com.fluxtream.services.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fluxtream.auth.FlxUserDetails;
import com.fluxtream.domain.Guest;

public class ServiceUtils {

	public static Guest getCurrentGuest() {
		SecurityContext context = SecurityContextHolder.getContext();
		Authentication authentication = context.getAuthentication();
		FlxUserDetails details = (FlxUserDetails) authentication.getPrincipal();
		return details.getGuest();
	}

	public static String getUsername() {
		SecurityContext context = SecurityContextHolder.getContext();
		Authentication authentication = context.getAuthentication();
		FlxUserDetails details = (FlxUserDetails) authentication.getPrincipal();
		return details.getUsername();
	}
	
}
