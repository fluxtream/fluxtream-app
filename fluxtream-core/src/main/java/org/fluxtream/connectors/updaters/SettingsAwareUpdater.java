package org.fluxtream.connectors.updaters;

/**
 * User: candide
 * Date: 27/07/13
 * Time: 13:17
 */
public interface SettingsAwareUpdater {

    /**
     * This method will be called whenever a user has edited the settings for a specific
     * connector instance. It will let the Updater do connector-specific things that
     * pertain to the handling of these settings like e.g. update the timeline styles
     * @param apiKeyId
     * @param settings
     */
    void connectorSettingsChanged(final long apiKeyId, final Object settings);

    /**
     * This is called after a successful connector update (historical or incremental) and
     * lets the updater update the provided settings with the freshest data.
     * @param updateInfo
     * @param settings either existing settings or a empty default settings instance
     */
    Object syncConnectorSettings(final UpdateInfo updateInfo, Object settings);

}
