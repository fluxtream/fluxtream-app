package org.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.Connector;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Type;

@Entity(name = "Connector")
// @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
    @NamedQuery(name = "connectors.all", query = "SELECT connector FROM Connector connector ORDER BY connector.count DESC"),
    @NamedQuery(name = "connector.byName", query = "SELECT connector FROM Connector connector WHERE connectorName=?"),
    @NamedQuery(name = "connector.deleteAll", query = "DELETE FROM Connector")

})
public class ConnectorInfo extends AbstractEntity implements Comparable<ConnectorInfo> {

	public String name;
	public int count;
	public String connectUrl;
	public String image;
	public String connectorName;

    @Type(type = "yes_no")
    public boolean supportsRenewTokens = false;

	@Type(type = "yes_no")
	public boolean enabled;

    @Type(type = "yes_no")
    public boolean manageable = true;

	@Lob
	public String text;

    //non persistent fields
    public transient long lastSync;
    public transient long latestData;
    public transient boolean syncing;
    public transient boolean errors;

    public String[] channels;

    public int api;
    public String renewTokensUrlTemplate;

    @Lob
    String apiKeyAttributeKeys;

    @Type(type = "yes_no")
    public boolean supportsFileUpload = false;

    @Type(type = "yes_no")
    public boolean supportsSync = true;

    public ConnectorInfo() {
	}

    public String[] getApiKeyAttributesKeys() {
        if(apiKeyAttributeKeys!=null) {
            final String[] keys = StringUtils.split(apiKeyAttributeKeys, ",");
            return keys;
        }
        return null;
    }

	public ConnectorInfo(String name, String imageUrl, String text,
			String connectUrl, Connector api, int count, boolean enabled,
            boolean supportsFileUpload, boolean supportsSync,
            String[] apiKeyAttributeKeys) {
		this.connectUrl = connectUrl;
		this.image = imageUrl;
		this.name = name;
		this.text = text;
		this.api = api.value();
		this.count = count;
		this.connectorName = api.getName();
		this.enabled = enabled;
        this.supportsFileUpload = supportsFileUpload;
        this.supportsSync = supportsSync;
        if (apiKeyAttributeKeys!=null)
            this.apiKeyAttributeKeys = StringUtils.join(apiKeyAttributeKeys, ",");
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

    @Override
    public int compareTo(final ConnectorInfo o) {
        return connectorName.compareTo(o.connectorName);
    }
}
