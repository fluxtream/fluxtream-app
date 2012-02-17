package com.fluxtream.updaters.strategies;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;

public interface UpdateStrategy {

	public UpdateInfo getUpdateInfo(ApiKey apiKey, int objectTypes,
			TimeInterval timeInterval);
	
}
