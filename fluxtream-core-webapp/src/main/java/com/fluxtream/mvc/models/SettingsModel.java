package com.fluxtream.mvc.models;

import com.fluxtream.domain.GuestSettings;

public class SettingsModel {

	String temperatureUnit, distanceUnit;
	
	public SettingsModel(GuestSettings settings) {
		this.temperatureUnit = settings.temperatureUnit.name();
		this.distanceUnit = settings.distanceMeasureUnit.name();
	}
	
}
