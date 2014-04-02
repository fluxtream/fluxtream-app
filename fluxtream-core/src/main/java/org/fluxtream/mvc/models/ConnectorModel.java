package org.fluxtream.mvc.models;


public class ConnectorModel {

	public String connectorName;
	public String prettyName;
    public long apiKeyId;
	
	public String getConnectorName() {
		return connectorName;
	}
	
	public String getPrettyName() {
		return prettyName;
	}

    public long apiKeyId() { return apiKeyId; }
	
}
