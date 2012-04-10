package com.fluxtream.widgets.dataproviders.day;

import net.sf.json.JSONObject;

import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.widgets.dataproviders.AbstractWidgetDataProvider;

@Component("day/caloriesBurned")
public class CaloriesBurnedDataProvider extends AbstractWidgetDataProvider {

	@Override
	public void provideData(long guestId, TimeInterval timeInterval,
			JSONObject o) {
		o.accumulate("caloriesBurnt", 2213);
	}
	
}
