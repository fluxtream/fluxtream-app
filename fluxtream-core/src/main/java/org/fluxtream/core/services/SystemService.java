package org.fluxtream.core.services;

import java.util.List;

import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.domain.ConnectorInfo;

public interface SystemService {

	public List<ConnectorInfo> getConnectors() throws Exception;

    public ConnectorInfo getConnectorInfo(String connectorName) throws Exception;
	
	public Connector getApiFromGoogleScope(String scope);

}
