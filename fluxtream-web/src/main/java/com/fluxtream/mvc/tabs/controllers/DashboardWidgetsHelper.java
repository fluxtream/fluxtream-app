package com.fluxtream.mvc.tabs.controllers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fluxtream.TimeInterval;
import com.fluxtream.dashboard.dataproviders.AbstractWidgetDataProvider;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.GuestSettings;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.SettingsService;

public class DashboardWidgetsHelper {

	@Autowired
	GuestService guestService;

	@Autowired
	BeanFactory beanFactory;
	
	@Autowired
	SettingsService settingsService;
	
	public class DashboardWidget {
		public int columns;
		public String name;
	}

	PropertiesConfiguration widgetProperties;
	Map<String, AbstractWidgetDataProvider> widgetDataProviders = new Hashtable<String, AbstractWidgetDataProvider>();

	public DashboardWidgetsHelper(PropertiesConfiguration widgetProperties) {
		this.widgetProperties = widgetProperties;
	}
	
	protected List<DashboardWidget> getAvailableUserWidgets(long guestId) {
		List<DashboardWidget> allWidgets = getAllWidgets();
		List<ApiKey> apiKeys = guestService.getApiKeys(guestId);
		List<DashboardWidget> availableUserWidgets = new ArrayList<DashboardWidget>();
		for (DashboardWidget widget : allWidgets) {
			if (isAvailableToUser(widget, apiKeys))
				availableUserWidgets.add(widget);
		}
		return availableUserWidgets;
	}

	private boolean isAvailableToUser(DashboardWidget widget, List<ApiKey> apiKeys) {
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

	private List<DashboardWidget> getAllWidgets() {
		Iterator<String> keys = widgetProperties.getKeys();
		Set<String> widgetNames = new HashSet<String>();
		while (keys.hasNext()) {
			String key = keys.next();
			String[] splits = key.split("\\.");
			String widgetName = splits[0];
			widgetNames.add(widgetName);
		}
		List<DashboardWidget> allWidgets = new ArrayList<DashboardWidget>();
		for (String widgetName : widgetNames) {
			int columns = widgetProperties.getInt(widgetName+".columns");
			DashboardWidget widget = new DashboardWidget();
			widget.name = widgetName;
			widget.columns = columns;
			allWidgets.add(widget);
		}
		return allWidgets;
	}

	public void provideWidgetsData(List<DashboardWidget> userWidgets, long guestId,
			TimeInterval timeInterval, JSONObject o) {
		String timeUnit = timeInterval.timeUnit.name().toLowerCase();
		GuestSettings settings = settingsService.getSettings(guestId);
		o.accumulate(AbstractWidgetDataProvider.REQUIRED, new JSONArray());
		for (DashboardWidget userWidget : userWidgets) {
			String dataProviderName = timeUnit + "/" + userWidget.name;
			if (!this.widgetDataProviders.containsKey(dataProviderName)) {
				AbstractWidgetDataProvider dataProviderBean = (AbstractWidgetDataProvider) beanFactory
						.getBean(dataProviderName);
				this.widgetDataProviders
						.put(dataProviderName, dataProviderBean);
			}
			AbstractWidgetDataProvider dataProvider = this.widgetDataProviders.get(dataProviderName);
			JSONObject widgetData = dataProvider.provideData(guestId, settings,
					timeInterval);
			handleRequired(o, widgetData);
			widgetData.accumulate("columns", userWidget.columns);
			o.accumulate(userWidget.name, widgetData);
		}
	}

	private void handleRequired(JSONObject o, JSONObject widgetData) {
		if (widgetData.has(AbstractWidgetDataProvider.REQUIRED)) {
			JSONArray required = o.getJSONArray(AbstractWidgetDataProvider.REQUIRED);
			JSONArray widgetRequired = widgetData.getJSONArray(AbstractWidgetDataProvider.REQUIRED);
			for (int i=0; i<widgetRequired.size(); i++)
				required.add(widgetRequired.getString(i));
			widgetData.remove(AbstractWidgetDataProvider.REQUIRED);
		}
	}
	
}
