package org.fluxtream.core.connectors.updaters;

import org.fluxtream.core.domain.SharedConnector;

/**
 * User: candide
 * Date: 27/07/13
 * Time: 13:17
 */
public interface SharedConnectorSettingsAwareUpdater {

    /**
     * This is called after a successful connector update (historical or incremental) and
     * lets the updater update the provided settings with the freshest data.
     * @param apiKeyId
     * @param sharedConnector
     */
    void syncSharedConnectorSettings(final long apiKeyId, final SharedConnector sharedConnector);

}
