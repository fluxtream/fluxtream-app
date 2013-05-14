package com.fluxtream.mvc.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.domain.metadata.WeatherInfo;
import com.sun.jersey.core.util.StringIgnoreCaseKeyComparator;

public class DigestModel {

	public TimeBoundariesModel tbounds;
	public SolarInfoModel solarInfo;
	public int nApis;
	//public Set<String> updateNeeded = new HashSet<String>();
	public boolean hasPictures;
	public List<NotificationModel> notifications;
	public Map<String,Collection> addresses;
	public List<DayMetadataFacet.VisitedCity> cities;
	public DayMetadataFacet.InTransitType inTransit;
	public DayMetadataFacet.TravelType travelType;
	public float minTempC, maxTempC;
	public float minTempF, maxTempF;
	public SettingsModel settings;
	public Set<String> haveDataConnectors = new HashSet<String>();
	public Set<String> haveNoDataConnectors = new HashSet<String>();
	public List<ConnectorDigestModel> selectedConnectors = new ArrayList<ConnectorDigestModel>();
    public List<WeatherInfo> hourlyWeatherData = null;
    public String timeUnit;
    public long timeZoneOffset;
    public List<GuestModel> coachees;
    public long generationTimestamp;

	@SuppressWarnings("rawtypes")
	public Map<String,Collection> cachedData
		= new HashMap<String,Collection>();

	public void addNotification(NotificationModel nm) {
		if (notifications == null)
			notifications = new ArrayList<NotificationModel>();
		notifications.add(nm);
	}
	
	public void setUpdateNeeded(String connectorName) {
		//updateNeeded.add(connectorName);
	}
	
	public void hasData(String connectorName, boolean b) {
		if (b) {
			haveDataConnectors.add(connectorName);
			if (haveNoDataConnectors.contains(connectorName))
				haveNoDataConnectors.remove(connectorName);
		}
		if (!b && !haveDataConnectors.contains(connectorName))
			haveNoDataConnectors.add(connectorName);
	}

}