package com.fluxtream.dashboard.dataproviders.week;

import org.springframework.stereotype.Component;

import net.sf.json.JSONObject;

import com.fluxtream.TimeInterval;
import com.fluxtream.dashboard.dataproviders.AbstractWidgetDataProvider;
import com.fluxtream.domain.GuestSettings;

@Component("week/hoursSlept")
public class HoursSleptDataProvider extends AbstractWidgetDataProvider {

	@Override
	public JSONObject provideData(long guestId, GuestSettings settings,
			TimeInterval timeInterval) {
		JSONObject hoursSlept = new JSONObject();
		hoursSlept.accumulate("minutes", 33);
		hoursSlept.accumulate("hours", 7);
		return hoursSlept;
	}

}
