package com.fluxtream.updaters.strategies;

import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;

@Component
public class IncrementalUpdateStrategy extends AbstractUpdateStrategy {

	private int refreshDelay = -1;

	private int refreshDelay() {
		if (refreshDelay==-1) {
			refreshDelay = Integer.valueOf(env.get("incrementalUpdatesRefreshDelay"));
		}
		return refreshDelay;
	}
	
	/**
	 * refresh the feed only if we are looking at the most recent events and
	 * if the last updated was done before the last 5 minutes (refreshDelay)
	 */
	@Override
	public UpdateInfo computeUpdateInfo(ApiKey apiKey,
			int objectTypes, TimeInterval timeInterval) {
		ApiUpdate lastUpdate = connectorUpdateService.getLastUpdate(apiKey.getGuestId(), apiKey.getConnector());
		long now = System.currentTimeMillis();
		if (timeInterval.isMostRecent()) {
			if (lastUpdate!=null && (now-lastUpdate.ts<refreshDelay()))
				return UpdateInfo.noopUpdateInfo(apiKey, objectTypes);
			else
				return UpdateInfo.refreshFeedUpdateInfo(apiKey, objectTypes);
		}  else
			return UpdateInfo.noopUpdateInfo(apiKey, objectTypes);
	}

}
