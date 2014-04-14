package org.fluxtream.core.domain;

import org.fluxtream.core.connectors.updaters.AbstractUpdater;

public interface Updatable {

	public void update(AbstractUpdater updater, ApiKey apiKey);
	
}
