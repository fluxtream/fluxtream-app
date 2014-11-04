package org.fluxtream.core.mvc.models;

import com.wordnik.swagger.annotations.ApiModel;

import java.util.Map;

/**
 * User: candide
 * Date: 04/11/14
 * Time: 19:41
 */
@ApiModel(value = "A dashboard widget")
public class DashboardWidgetManifestModel {


    public String WidgetName;
    public String WidgetRepositoryURL;
    public Map<String, String> WidgetDescription;
    public Map<String, String> WidgetTitle;
    public String WidgetIcon;
    public boolean HasSettings;
}
