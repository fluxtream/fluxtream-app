package com.fluxtream.mvc.models;

import com.fluxtream.domain.GuestSettings;

public class SettingsModel {

	String temperatureUnit, distanceUnit, weightMeasureUnit, lengthMeasureUnit;
	
	public SettingsModel(GuestSettings settings) {
		this.temperatureUnit = settings.temperatureUnit.name();
		this.distanceUnit = settings.distanceMeasureUnit.name();
        this.weightMeasureUnit = settings.weightMeasureUnit.name();
        this.lengthMeasureUnit = settings.lengthMeasureUnit.name();
	}
	
}
