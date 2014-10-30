package org.fluxtream.core.services;

import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;

import java.util.List;

public interface BodyTrackStorageService {

	public void storeInitialHistory(ApiKey apiKey);

    public void storeInitialHistory(ApiKey apiKey, int objectTypes);

	public void storeApiData(long guestId, List<AbstractFacet> facet);
	
}
