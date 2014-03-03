package org.fluxtream.mvc.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fluxtream.Configuration;
import org.fluxtream.TimeUnit;
import org.fluxtream.metadata.AbstractTimespanMetadata;

public class DigestModel {

	public TimeBoundariesModel tbounds;
	public int nApis;
	public boolean hasPictures;
	public List<NotificationModel> notifications;
    public Map<String,Collection> addresses;
	public SettingsModel settings;
	public Set<String> haveDataConnectors = new HashSet<String>();
	public Set<String> haveNoDataConnectors = new HashSet<String>();
	public List<ConnectorDigestModel> selectedConnectors = new ArrayList<ConnectorDigestModel>();
    public List<GuestModel> coachees;
    public long generationTimestamp;

    public Metadata metadata;

    public DigestModel(TimeUnit timeUnit, AbstractTimespanMetadata metadata, Configuration env) {
        VisitedCityModel nic = null, pic = null;
        if (metadata.nextInferredCity!=null)
            nic = new VisitedCityModel(metadata.nextInferredCity, env);
        if (metadata.previousInferredCity!=null)
            pic = new VisitedCityModel(metadata.previousInferredCity, env);
        this.metadata = new Metadata(timeUnit.toString(), pic, nic);
    }

    public class Metadata {

        public SolarInfoModel solarInfo;

        Metadata(String timeUnit, VisitedCityModel previousInferredCity, VisitedCityModel nextInferredCity) {
            this.timeUnit = timeUnit;
            this.previousInferredCity = previousInferredCity;
            this.nextInferredCity = nextInferredCity;
        }

        public String timeUnit;
        public List<VisitedCityModel> cities = new ArrayList<VisitedCityModel>();
        public List<VisitedCityModel> consensusCities = new ArrayList<VisitedCityModel>();
        public VisitedCityModel previousInferredCity;
        public VisitedCityModel nextInferredCity;
        public VisitedCityModel mainCity;
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