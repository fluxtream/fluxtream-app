package org.fluxtream.core.mvc.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.connectors.vos.AbstractFacetVO;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.metadata.AbstractTimespanMetadata;

@ApiModel(value = "Generic data model for CalendarData Store operations")
public class DigestModel {

    @JsonRawValue
    public String calendar;
	public TimeBoundariesModel tbounds;
	public int nApis;

    @ApiModelProperty(value="The list of this user's addresses", required=true)
    public Map<String,List<AddressModel>> addresses;

    @ApiModelProperty(value="The list of this user's trustingBuddies", required=true)
	public SettingsModel settings;

    @ApiModelProperty(value="Background updates notifications, if any", required=false)
    public List<NotificationModel> notifications;

    @ApiModelProperty(value="List of names of connectors that have data for the given time boundaries", required=true)
	public List<String> haveDataConnectors = new ArrayList<String>();

    @ApiModelProperty(value="List of names of connectors that don't have any data for the given time boundaries", required=true)
	public List<String> haveNoDataConnectors = new ArrayList<String>();

    @ApiModelProperty(value="List of currently selected connectors", required=true)
	public List<ConnectorDigestModel> selectedConnectors = new ArrayList<ConnectorDigestModel>();

    @ApiModelProperty(value="The list of this user's trustingBuddies", required=true)
    public List<GuestModel> trustingBuddies;

    @ApiModelProperty(value="UTC timestamp of this model's generation", required=true)
    public long generationTimestamp;

    public Map<String,Collection<AbstractFacetVO<AbstractFacet>>> facets
            = new HashMap<String,Collection<AbstractFacetVO<AbstractFacet>>>();

    public Metadata metadata;

    public DigestModel(TimeUnit timeUnit, AbstractTimespanMetadata metadata, Configuration env, CalendarModel calendarModel) {
        VisitedCityModel nic = null, pic = null;
        if (metadata.nextInferredCity!=null)
            nic = new VisitedCityModel(metadata.nextInferredCity, env);
        if (metadata.previousInferredCity!=null)
            pic = new VisitedCityModel(metadata.previousInferredCity, env);
        this.metadata = new Metadata(timeUnit.toString(), pic, nic);
        this.calendar = calendarModel.toJSONString(env);
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