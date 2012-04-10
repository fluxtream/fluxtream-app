package com.fluxtream.widgets.dataproviders;

import com.fluxtream.TimeInterval;

import net.sf.json.JSONObject;

public abstract class AbstractWidgetDataProvider {

	public abstract void provideData(long guestId, TimeInterval timeInterval, JSONObject o);
	
}
