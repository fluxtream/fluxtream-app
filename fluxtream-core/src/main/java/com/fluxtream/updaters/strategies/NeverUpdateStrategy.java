package com.fluxtream.updaters.strategies;

import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;

@Component
public class NeverUpdateStrategy extends AbstractUpdateStrategy {

	/**
	 * Never refresh!
	 */
	@Override
	public UpdateInfo computeUpdateInfo(ApiKey apiKey, int objectTypes,
			TimeInterval timeInterval) {
		
		return UpdateInfo.noopUpdateInfo(apiKey, objectTypes);
	}
	
}
