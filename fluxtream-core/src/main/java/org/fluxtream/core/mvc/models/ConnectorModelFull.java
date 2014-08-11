package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * User: candide
 * Date: 25/06/14
 * Time: 12:59
 */
@ApiModel(value = "Full info on a connector (instance).")
public class ConnectorModelFull {

    @ApiModelProperty("Types of faces provided by this connector")
    public List<String> facetTypes;

    @ApiModelProperty(value = "Current status of the connector", allowableValues="STATUS_UP, STATUS_PERMANENT_FAILURE, STATUS_TRANSIENT_FAILURE, STATUS_OVER_RATE_LIMIT")
    public String status;

    @ApiModelProperty("User name of the connector")
    public String name;

    @ApiModelProperty("URL to direct the user to to add an instance of this connector")
    public String connectUrl;

    @ApiModelProperty("Icon URL for this connector")
    public String image;

    @ApiModelProperty("The connector's 'technical' name")
    public String connectorName;

    @ApiModelProperty("Wether this connector is enabled on this server")
    public boolean enabled;

    @ApiModelProperty("Wether this connector is manageable")
    public boolean manageable;

    @ApiModelProperty("User-friendly description of the device or service related to this connector")
    public String text;

    @ApiModelProperty("This connector's 'technical' api code")
    public int api;

    @ApiModelProperty("This connector instance's API key ID")
    public Long apiKeyId;

    @ApiModelProperty("UTC timestamp of this connector's last sync time")
    public long lastSync;

    @ApiModelProperty("UTC timestamp of this connector's latest data")
    public long latestData;

    @ApiModelProperty("Possible error trace if this connector is currently down")
    public boolean errors;

    @ApiModelProperty("Types of faces provided by this connector")
    public String auditTrail;

    @ApiModelProperty("Is this connector currently synching?")
    public boolean syncing;

    @ApiModelProperty("The list of BodyTrack channels associated to this connector")
    public String[] channels;

    @ApiModelProperty("Types of faces provided by this connector")
    public boolean sticky;

    @ApiModelProperty("Wether this connector supports oauth tokens renewal")
    public boolean supportsRenewToken;

    @ApiModelProperty("Wether this connector supports sync")
    public boolean supportsSync;

    @ApiModelProperty("Wether this connector supports file upload")
    public boolean supportsFileUpload;

    @ApiModelProperty("This connector's pretty name")
    public String prettyName;

    @ApiModelProperty("If this is an upload connector, the message that is shown to the user on the dialog that allows file upload")
    public String uploadMessage;
}
