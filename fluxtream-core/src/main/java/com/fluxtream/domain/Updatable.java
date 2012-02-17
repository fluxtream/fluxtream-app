package com.fluxtream.domain;

import com.fluxtream.connectors.updaters.AbstractUpdater;

public interface Updatable {

	public void update(AbstractUpdater updater, ApiKey apiKey);
	
}
