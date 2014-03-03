package org.fluxtream.services;

import java.util.List;

import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.ConnectorInfo;

public interface SystemService {

	public List<ConnectorInfo> getConnectors() throws Exception;

    public ConnectorInfo getConnectorInfo(String connectorName) throws Exception;
	
	public Connector getApiFromGoogleScope(String scope);
	
}
