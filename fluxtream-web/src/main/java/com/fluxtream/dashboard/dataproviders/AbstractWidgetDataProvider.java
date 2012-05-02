package com.fluxtream.dashboard.dataproviders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.GuestService;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public abstract class AbstractWidgetDataProvider {

	public static final String REQUIRED = "required";

	@Autowired
	protected ApiDataService apiDataService;

	@Autowired
	protected GuestService guestService;

	@Autowired
	protected Configuration env;

	public abstract JSONObject provideData(long guestId,
			GuestSettings settings, TimeInterval timeInterval);

	protected void addRequiredJS(JSONObject o, String scriptName) {
		if (!o.has(REQUIRED))
			o.accumulate(REQUIRED, new JSONArray());
		Component component = this.getClass().getAnnotation(Component.class);
		String dashboardWidgetName = component.value();
		String timeUnit = dashboardWidgetName.split("/")[0];
		o.getJSONArray(REQUIRED).add(
				"/" + env.get("release") + "/tabs/dashboard/" + timeUnit
						+ "/" + scriptName + ".js");
	}

}
