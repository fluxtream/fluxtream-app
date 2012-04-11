package com.fluxtream.mvc.widgets.controllers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.TimeInterval;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;
import com.fluxtream.widgets.dataproviders.AbstractWidgetDataProvider;

public class StatsHelper {

	@Autowired
	GuestService guestService;

	@Autowired
	BeanFactory beanFactory;
	
	@Autowired
	SettingsService settingsService;
	
	public class Widget {
		public int columns;
		public String name;
	}

	PropertiesConfiguration widgetProperties;
	Map<String, AbstractWidgetDataProvider> widgetDataProviders = new Hashtable<String, AbstractWidgetDataProvider>();

	public StatsHelper(PropertiesConfiguration widgetProperties) {
		this.widgetProperties = widgetProperties;
	}
	
	protected List<Widget> getAvailableUserWidgets(long guestId) {
		List<Widget> allWidgets = getAllWidgets();
		List<ApiKey> apiKeys = guestService.getApiKeys(guestId);
		List<Widget> availableUserWidgets = new ArrayList<Widget>();
		for (Widget widget : allWidgets) {
			if (isAvailableToUser(widget, apiKeys))
				availableUserWidgets.add(widget);
		}
		return availableUserWidgets;
	}

	private boolean isAvailableToUser(Widget widget, List<ApiKey> apiKeys) {
		String[] requiredConnectors = widgetProperties.getStringArray(widget.name
				+ ".requiredConnectors");
		for (String requiredConnector : requiredConnectors) {
			for (ApiKey apiKey : apiKeys) {
				if (apiKey.getConnector().getName()
						.equalsIgnoreCase(requiredConnector))
					return true;
			}
		}
		return false;
	}

	private List<Widget> getAllWidgets() {
		Iterator<String> keys = widgetProperties.getKeys();
		Set<String> widgetNames = new HashSet<String>();
		while (keys.hasNext()) {
			String key = keys.next();
			String[] splits = key.split("\\.");
			String widgetName = splits[0];
			widgetNames.add(widgetName);
		}
		List<Widget> allWidgets = new ArrayList<Widget>();
		for (String widgetName : widgetNames) {
			int columns = widgetProperties.getInt(widgetName+".columns");
			Widget widget = new Widget();
			widget.name = widgetName;
			widget.columns = columns;
			allWidgets.add(widget);
		}
		return allWidgets;
	}

	public void provideWidgetsData(List<Widget> userWidgets, long guestId,
			TimeInterval timeInterval, JSONObject o) {
		String timeUnit = timeInterval.timeUnit.name().toLowerCase();
		GuestSettings settings = settingsService.getSettings(guestId);
		for (Widget userWidget : userWidgets) {
			String dataProviderName = timeUnit + "/" + userWidget.name;
			if (!this.widgetDataProviders.containsKey(dataProviderName)) {
				AbstractWidgetDataProvider dataProviderBean = (AbstractWidgetDataProvider) beanFactory
						.getBean(dataProviderName);
				this.widgetDataProviders
						.put(dataProviderName, dataProviderBean);
			}
			JSONObject widgetData = this.widgetDataProviders.get(dataProviderName).provideData(guestId, settings,
					timeInterval);
			widgetData.accumulate("columns", userWidget.columns);
			o.accumulate(userWidget.name, widgetData);
		}
	}
	
}
