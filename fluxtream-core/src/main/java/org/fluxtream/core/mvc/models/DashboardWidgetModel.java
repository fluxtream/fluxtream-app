package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.codehaus.jackson.annotate.JsonRawValue;

/**
 * User: candide
 * Date: 04/11/14
 * Time: 19:45
 */
@ApiModel("A widget (after it was added to a dashboard)")
public class DashboardWidgetModel {

    @JsonRawValue
    @ApiModelProperty(value="Free-form JSON that contains this widget's settings", required=true)
    public String settings;

    @ApiModelProperty(value="Some basic info about this widget", required=true)
    public DashboardWidgetManifestModel manifest;

}
