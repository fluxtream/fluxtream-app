package org.fluxtream.core.mvc.models;

/**
 * User: candide
 * Date: 06/07/14
 * Time: 21:41
 */
public class SharedConnectorModel {

    public String connectorName;
    public boolean shared;
    public long apiKeyId;
    public boolean hasSettings;

    public SharedConnectorModel() {}

    public String prettyName;
}
