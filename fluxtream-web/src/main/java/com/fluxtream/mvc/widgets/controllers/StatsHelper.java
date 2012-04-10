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
import com.fluxtream.services.GuestService;
import com.fluxtream.widgets.dataproviders.AbstractWidgetDataProvider;

public class StatsHelper {

	@Autowired
	GuestService guestService;

	@Autowired
	BeanFactory beanFactory;

	PropertiesConfiguration widgetProperties;
	Map<String, AbstractWidgetDataProvider> widgetDataProviders = new Hashtable<String, AbstractWidgetDataProvider>();

	public StatsHelper(PropertiesConfiguration widgetProperties) {
		this.widgetProperties = widgetProperties;
	}
	
	protected List<String> getAvailableUserWidgets(long guestId) {
		List<String> allWidgets = getAllWidgets();
		List<ApiKey> apiKeys = guestService.getApiKeys(guestId);
		List<String> availableUserWidgets = new ArrayList<String>();
		for (String widget : allWidgets) {
			if (isAvailableToUser(widget, apiKeys))
				availableUserWidgets.add(widget);
		}
		return availableUserWidgets;
	}

	private boolean isAvailableToUser(String widget, List<ApiKey> apiKeys) {
		String[] requiredConnectors = widgetProperties.getStringArray(widget
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

	private List<String> getAllWidgets() {
		Iterator<String> keys = widgetProperties.getKeys();
		Set<String> widgetNames = new HashSet<String>();
		while (keys.hasNext()) {
			String key = keys.next();
			String[] splits = key.split("\\.");
			String widgetName = splits[0];
			widgetNames.add(widgetName);
		}
		return new ArrayList<String>(widgetNames);
	}

	public void provideWidgetsData(List<String> userWidgets, long guestId,
			TimeInterval timeInterval, JSONObject o) {
		String timeUnit = timeInterval.timeUnit.name().toLowerCase();
		for (String userWidget : userWidgets) {
			String dataProviderName = timeUnit + "/" + userWidget;
			if (!this.widgetDataProviders.containsKey(dataProviderName)) {
				AbstractWidgetDataProvider dataProviderBean = (AbstractWidgetDataProvider) beanFactory
						.getBean(dataProviderName);
				this.widgetDataProviders
						.put(dataProviderName, dataProviderBean);
			}
			this.widgetDataProviders.get(dataProviderName).provideData(guestId,
					timeInterval, o);
		}
	}
	
}
