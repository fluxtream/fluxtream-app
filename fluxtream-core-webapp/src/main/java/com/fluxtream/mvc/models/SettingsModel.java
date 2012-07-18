package com.fluxtream.mvc.models;

import com.fluxtream.domain.Guest;
import com.fluxtream.domain.GuestSettings;

public class SettingsModel {

	String firstName, lastName, temperatureUnit, distanceUnit, weightMeasureUnit, lengthMeasureUnit;
	
	public SettingsModel(GuestSettings settings, Guest guest) {
        this.firstName = guest.firstname;
        this.lastName = guest.lastname;
		this.temperatureUnit = settings.temperatureUnit.name();
		this.distanceUnit = settings.distanceMeasureUnit.name();
        this.weightMeasureUnit = settings.weightMeasureUnit.name();
        this.lengthMeasureUnit = settings.lengthMeasureUnit.name();
	}
	
}
