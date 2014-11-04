package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * User: candide
 * Date: 04/11/14
 * Time: 19:40
 */
@ApiModel(value = "A widgets dashboard")
public class DashboardModel {

    @ApiModelProperty(value="Dashboard ID", required=true)
    public Long id;
    @ApiModelProperty(value="Used-assigned name", required=true)
    public String name;
    @ApiModelProperty(value="Is this the current/active dashboard", required=true)
    public boolean active;
    @ApiModelProperty(value="Widgets contained under this dashboard", required=true)
    public List<DashboardWidgetModel> widgets;

}
