package com.fluxtream.connectors.updaters;

import com.fluxtream.domain.ApiKey;

/**
 * User: candide
 * Date: 27/07/13
 * Time: 13:17
 */
public abstract class SettingsAwareAbstractUpdater extends AbstractUpdater {

    public abstract Object getSettings(ApiKey apiKey);

}
