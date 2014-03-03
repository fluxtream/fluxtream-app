package org.fluxtream.domain;

import org.fluxtream.connectors.updaters.AbstractUpdater;

public interface Updatable {

	public void update(AbstractUpdater updater, ApiKey apiKey);
	
}
