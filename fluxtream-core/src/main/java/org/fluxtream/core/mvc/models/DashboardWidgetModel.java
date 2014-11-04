package org.fluxtream.core.mvc.models;

import org.codehaus.jackson.annotate.JsonRawValue;

/**
 * User: candide
 * Date: 04/11/14
 * Time: 19:45
 */
public class DashboardWidgetModel {

    @JsonRawValue
    public String settings;

    public DashboardWidgetManifestModel manifest;

}
