package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name = "Settings")
@NamedQueries({
	@NamedQuery(name = "settings.byGuestId",
		query = "SELECT settings FROM Settings settings WHERE settings.guestId=?"),
	@NamedQuery(name = "settings.delete.all",
		query = "DELETE FROM Settings settings WHERE settings.guestId=?")
})
public class GuestSettings extends AbstractEntity {

	public GuestSettings() {
	}

	public enum WeightMeasureUnit {
		SI, POUNDS, STONES
	}

	public enum LengthMeasureUnit {
		SI, FEET_INCHES
	}

	public enum DistanceMeasureUnit {
		SI, MILES_YARDS
	}
	
	public enum TemperatureUnit {
		CELSIUS, FAHRENHEIT
	}

	public TemperatureUnit temperatureUnit = TemperatureUnit.FAHRENHEIT;

	public WeightMeasureUnit weightMeasureUnit = WeightMeasureUnit.POUNDS;

	public LengthMeasureUnit lengthMeasureUnit = LengthMeasureUnit.FEET_INCHES;

	public DistanceMeasureUnit distanceMeasureUnit = DistanceMeasureUnit.MILES_YARDS;

	public long guestId;

}
