package com.fluxtream.widgets.dataproviders.week;

import org.springframework.stereotype.Component;

import net.sf.json.JSONObject;

import com.fluxtream.TimeInterval;
import com.fluxtream.widgets.dataproviders.AbstractWidgetDataProvider;

@Component("week/hoursSlept")
public class HoursSleptDataProvider extends AbstractWidgetDataProvider {

	@Override
	public void provideData(long guestId, TimeInterval timeInterval,
			JSONObject o) {
		JSONObject hoursSlept = new JSONObject();
		hoursSlept.accumulate("minutes", 33);
		hoursSlept.accumulate("hours", 7);
		o.accumulate("hoursSlept", hoursSlept);
	}

}
