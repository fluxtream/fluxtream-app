package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;

import com.fluxtream.connectors.Connector;

@Entity(name = "Connector")
// @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({ @NamedQuery(name = "connectors.all", query = "SELECT connector FROM Connector connector ORDER BY connector.count DESC") })
public class ConnectorInfo extends AbstractEntity {

	public String name;
	public int count;
	public String connectUrl;
	public String image;
	public String connectorName;
	@Type(type = "yes_no")
	public boolean enabled;

	@Lob
	public String text;

    //non persistent fields
    public long lastSync;
    public long latestData;
    public boolean syncing;
    public boolean errors;

    public int api;

	public ConnectorInfo() {
	}

	public ConnectorInfo(String name, String imageUrl, String text,
			String connectUrl, Connector api, int count, boolean enabled) {
		this.connectUrl = connectUrl;
		this.image = imageUrl;
		this.name = name;
		this.text = text;
		this.api = api.value();
		this.count = count;
		this.connectorName = api.getName();
		this.enabled = enabled;
	}

	public boolean equals(Object o) {
		ConnectorInfo c = (ConnectorInfo) o;
		return c.api == api;
	}

	public String getName() {
		return name;
	}

	public int getCount() {
		return count;
	}

	public String getConnectUrl() {
		return connectUrl;
	}

	public String getImage() {
		return image;
	}

	public String getText() {
		return text;
	}

	public Connector getApi() {
		return Connector.fromValue(api);
	}

}
