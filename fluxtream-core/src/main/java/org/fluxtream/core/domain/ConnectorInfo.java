package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;
import org.fluxtream.core.connectors.Connector;
import org.hibernate.annotations.Type;

@Entity(name = "Connector")
// @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
    @NamedQuery(name = "connectors.all", query = "SELECT connector FROM Connector connector ORDER BY connector.count DESC"),
    @NamedQuery(name = "connector.byName", query = "SELECT connector FROM Connector connector WHERE connectorName=?"),
    @NamedQuery(name = "connector.deleteAll", query = "DELETE FROM Connector")

})
@ApiModel(value = "Both generic and instance-specific information about a connector")
public class ConnectorInfo extends AbstractEntity implements Comparable<ConnectorInfo> {

    @ApiModelProperty(value = "The connector's user-friendly name", required = true)
	public String name;

	public int count;

    @ApiModelProperty(value = "URL to direct the user to for connector addition", required = true)
	public String connectUrl;

    @ApiModelProperty(value = "URL of this connector's logo", required = true)
	public String image;

    @ApiModelProperty(value = "The connector's 'technical' name", required = true)
	public String connectorName;

    @Type(type = "yes_no")
    @ApiModelProperty(value = "Wether this connector supports renewing its tokens", required = true)
    public boolean supportsRenewTokens = false;

	@Type(type = "yes_no")
    @ApiModelProperty(value = "Wether this connector is enabled", required = true)
	public boolean enabled;

    @Type(type = "yes_no")
    @ApiModelProperty(value = "Wether this connector is manageable", required = true)
    public boolean manageable = true;

	@Lob
    @ApiModelProperty(value = "User-friendly description of the device or service related to this connector", required = true)
	public String text;

    //non persistent fields
    @ApiModelProperty(value = "UTC timestamp of this connector's last sync time", required = true)
    public transient long lastSync;

    @ApiModelProperty(value = "UTC timestamp of this connector's latest data", required = true)
    public transient long latestData;

    @ApiModelProperty(value = "Is this connector currently synching?", required = true)
    public transient boolean syncing;

    @ApiModelProperty(value = "Possible error trace if this connector is currently down", required = false)
    public transient boolean errors;

    @ApiModelProperty(value = "The list of BodyTrack channels associated to this connector", required = true)
    public String[] channels;

    @ApiModelProperty(value = "This connector's 'technical' api code", required = true)
    public int api;

    @ApiModelProperty(value = "URL template to direct the user to for tokens renewal", required = true)
    public String renewTokensUrlTemplate;

    @Lob
    String apiKeyAttributeKeys;

    @Type(type = "yes_no")
    @ApiModelProperty(value = "Wether this connector supports file upload", required = true)
    public boolean supportsFileUpload = false;

    @Type(type = "yes_no")
    @ApiModelProperty(value = "Wether this connector supports synchronization", required = true)
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
