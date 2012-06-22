package com.fluxtream.mvc.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpException;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fluxtream.Configuration;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.utils.HttpUtils;
import com.google.gson.Gson;

@Controller
public class SettingsController {

	@Autowired
	GuestService guestService;

	@Autowired
	MetadataService metadataService;

	@Autowired
	SettingsService settingsService;

	@Autowired
	Configuration env;

	private static final DateTimeFormatter datePickerDateFormatter = DateTimeFormat
			.forPattern("MM/dd/yyyy");

	private final Gson gson = new Gson();

	@RequestMapping("/settings/main")
	public ModelAndView getSettings(HttpServletRequest request,
			HttpServletResponse response) {
		long guestId = ControllerHelper.getGuestId();
		Guest guest = guestService.getGuestById(guestId);
		GuestSettings settings = settingsService.getSettings(guestId);
		ModelAndView mav = new ModelAndView("settings/index");
		mav.addObject("settings", settings);
		mav.addObject("firstname", guest.firstname == null ? ""
				: guest.firstname);
		mav.addObject("lastname", guest.lastname == null ? "" : guest.lastname);
		return mav;
	}

	@RequestMapping("/settings/save")
	public void saveSettings(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String firstname = request.getParameter("guest_firstname");
		String lastname = request.getParameter("guest_lastname");
		String length_measure_unit = request
				.getParameter("length_measure_unit");
		String weight_measure_unit = request
				.getParameter("weight_measure_unit");
		String distance_measure_unit = request
				.getParameter("distance_measure_unit");
		String temperature_unit = request
				.getParameter("temperature_unit");

		GuestSettings.LengthMeasureUnit lengthUnit = Enum.valueOf(
				GuestSettings.LengthMeasureUnit.class, length_measure_unit);
		GuestSettings.DistanceMeasureUnit distanceUnit = Enum.valueOf(
				GuestSettings.DistanceMeasureUnit.class, distance_measure_unit);
		GuestSettings.WeightMeasureUnit weightUnit = Enum.valueOf(
				GuestSettings.WeightMeasureUnit.class, weight_measure_unit);
		GuestSettings.TemperatureUnit temperatureUnit = Enum.valueOf(
				GuestSettings.TemperatureUnit.class, temperature_unit); //

		long guestId = ControllerHelper.getGuestId();

		settingsService.setLengthMeasureUnit(guestId, lengthUnit);
		settingsService.setDistanceMeasureUnit(guestId, distanceUnit);
		settingsService.setWeightMeasureUnit(guestId, weightUnit);
		settingsService.setTemperatureUnit(guestId, temperatureUnit);

		settingsService.setFirstname(guestId, firstname);
		settingsService.setLastname(guestId, lastname);
		StatusModel status = new StatusModel(true, "firstname was set");
		String statusJson = gson.toJson(status);
		response.setContentType("application/json; charset=utf-8");
		response.getWriter().write(statusJson);
	}
}
