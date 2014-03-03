package org.fluxtream.utils;

import java.util.List;
import org.fluxtream.connectors.Connector;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.services.GuestService;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class ConnectorUtils {

    /** Returns the Connector having the given pretty name.  Returns <code>null</code> if no such connector exists. */
    public static Connector findConnectorByPrettyName(final GuestService guestService, final long guestId, final String connectorPrettyName) {
        List<ApiKey> userKeys = guestService.getApiKeys(guestId);
        for (ApiKey key : userKeys) {
            if (key != null) {
                final Connector connector = key.getConnector();
                if (connector != null && connector.prettyName() != null && connector.prettyName().equals(connectorPrettyName)) {
                    return connector;
                }
            }
        }

        return null;
    }

    private ConnectorUtils() {
        // private to prevent instantiation
    }
}
