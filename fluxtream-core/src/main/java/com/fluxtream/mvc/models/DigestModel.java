package com.fluxtream.mvc.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.WeatherInfo;

public class DigestModel {

	public TimeBoundariesModel tbounds;
	public SolarInfoModel solarInfo;
	public int nApis;
	public boolean hasPictures;
	public List<NotificationModel> notifications;
    public Map<String,Collection> addresses;
	public SettingsModel settings;
	public Set<String> haveDataConnectors = new HashSet<String>();
	public Set<String> haveNoDataConnectors = new HashSet<String>();
	public List<ConnectorDigestModel> selectedConnectors = new ArrayList<ConnectorDigestModel>();
    public List<WeatherInfo> hourlyWeatherData = null;
    public List<GuestModel> coachees;
    public long generationTimestamp;

    public Metadata metadata;

    public DigestModel(TimeUnit timeUnit) {
        metadata = new Metadata(timeUnit.toString());
    }

    public class Metadata {

        Metadata(String timeUnit) {
            this.timeUnit = timeUnit;
        }

        public String timeUnit;
        public List<VisitedCityModel> cities = new ArrayList<VisitedCityModel>();
        public VisitedCityModel mainCity;
        public float minTempC, maxTempC;
        public float minTempF, maxTempF;
        public long timeZoneOffset;
    }

	@SuppressWarnings("rawtypes")
	public Map<String,Collection> cachedData
		= new HashMap<String,Collection>();

	public void addNotification(NotificationModel nm) {
		if (notifications == null)
			notifications = new ArrayList<NotificationModel>();
		notifications.add(nm);
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