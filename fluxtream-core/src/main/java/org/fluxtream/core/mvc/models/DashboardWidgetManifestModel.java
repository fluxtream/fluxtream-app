package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * User: candide
 * Date: 04/11/14
 * Time: 19:41
 */
@ApiModel(value = "Basic dashboard widget info")
public class DashboardWidgetManifestModel {

    @ApiModelProperty(value="Name of the widget", required=true)
    public String WidgetName;
    @ApiModelProperty(value="Where does this widget come from?", required=true)
    public String WidgetRepositoryURL;
    @ApiModelProperty(value="Localized description", required=true)
    public Map<String, String> WidgetDescription;
    @ApiModelProperty(value="Localized title", required=true)
    public Map<String, String> WidgetTitle;
    @ApiModelProperty(value="Icon URL", required=true)
    public String WidgetIcon;
    @ApiModelProperty(value="Does it support settings?", required=true)
    public boolean HasSettings;
    @ApiModelProperty(value="Required Connectors", required=true)
    public List<String> RequiredConnectors;
    @ApiModelProperty(value="Whether or not the widget needs access to everything in the client", required=true)
    public boolean fullAccess;
}
