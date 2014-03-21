package org.fluxtream.mvc.models;

import java.util.Map;
import org.fluxtream.domain.Guest;
import org.fluxtream.domain.GuestSettings;

public class SettingsModel {

	String firstName, lastName, temperatureUnit, distanceUnit, weightMeasureUnit, lengthMeasureUnit;

    public Map<Long,Object> connectorSettings;
    public Map<String,Integer> messageDisplayCounters;
    public String registrationMethod;

	public SettingsModel(GuestSettings settings, Map<Long,Object> connectorSettings, Guest guest) {
        this.firstName = guest.firstname;
        this.lastName = guest.lastname;
        this.registrationMethod = guest.registrationMethod.name();
		this.temperatureUnit = settings.temperatureUnit.name();
		this.distanceUnit = settings.distanceMeasureUnit.name();
        this.weightMeasureUnit = settings.weightMeasureUnit.name();
        this.lengthMeasureUnit = settings.lengthMeasureUnit.name();
        this.connectorSettings = connectorSettings;
        this.messageDisplayCounters = settings.getMessageDisplayCounters();
	}
	
}
