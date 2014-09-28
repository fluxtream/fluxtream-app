package org.fluxtream.core.mvc.models;

import java.util.Map;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestSettings;

@ApiModel(value = "User Preferences: units of measure, first name and last name and connector settings.")
public class SettingsModel {

	String firstName, lastName, temperatureUnit, distanceUnit, weightMeasureUnit, lengthMeasureUnit;

    @ApiModelProperty(value = "Associative array of connector settings with apiKeyIds as keys and custom settings objects as values",
                      required=true)
    public Map<Long,Object> connectorSettings;

    @ApiModelProperty(value = "Associative array of connector settings with message ids as keys and display count as values",
                      required=true)
    public Map<String,Integer> messageDisplayCounters;

    @ApiModelProperty(value = "Method used for registering this user",
                      required=true, allowableValues = "REGISTRATION_METHOD_FORM, REGISTRATION_METHOD_FACEBOOK, REGISTRATION_METHOD_FACEBOOK_WITH_PASSWORD")
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
