package org.fluxtream.core.mvc.models;

import org.codehaus.jackson.annotate.JsonRawValue;
import org.fluxtream.core.domain.Guest;
import org.fluxtream.core.domain.GuestSettings;

import java.util.List;

/**
 * User: candide
 * Date: 11/06/14
 * Time: 14:29
 */
public class GuestSettingsModel {

    public String firstName, lastName;
    public Guest.RegistrationMethod registrationMethod;
    public String username;

    @JsonRawValue
    public String messageDisplayCountersStorage;

    public GuestSettings.TemperatureUnit temperatureUnit = GuestSettings.TemperatureUnit.FAHRENHEIT;

    public GuestSettings.WeightMeasureUnit weightMeasureUnit = GuestSettings.WeightMeasureUnit.POUNDS;

    public GuestSettings.LengthMeasureUnit lengthMeasureUnit = GuestSettings.LengthMeasureUnit.FEET_INCHES;

    public GuestSettings.DistanceMeasureUnit distanceMeasureUnit = GuestSettings.DistanceMeasureUnit.MILES_YARDS;

    public List<AuthorizationTokenModel> accessTokens;

    public GuestSettingsModel(final GuestSettings settings,
                              final String username, final String firstName, final String lastName,
                              final Guest.RegistrationMethod registrationMethod,
                              final List<AuthorizationTokenModel> accessTokens){
        this.temperatureUnit = settings.temperatureUnit;
        this.weightMeasureUnit = settings.weightMeasureUnit;
        this.lengthMeasureUnit = settings.lengthMeasureUnit;
        this.distanceMeasureUnit = settings.distanceMeasureUnit;
        this.messageDisplayCountersStorage = settings.messageDisplayCountersStorage;

        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registrationMethod = registrationMethod;
        this.accessTokens = accessTokens;
    }

}
