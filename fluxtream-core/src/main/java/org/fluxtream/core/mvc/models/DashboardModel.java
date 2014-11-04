package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;

import java.util.List;

/**
 * User: candide
 * Date: 04/11/14
 * Time: 19:40
 */
@ApiModel(value = "A widgets dashboard")
public class DashboardModel {

    public Long id;
    public String name;
    public boolean active;
    public List<DashboardWidgetModel> widgets;

}
