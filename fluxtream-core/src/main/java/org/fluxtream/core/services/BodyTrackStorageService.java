package org.fluxtream.core.services;

import java.util.List;

import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;

public interface BodyTrackStorageService {

	public void storeInitialHistory(ApiKey apiKey);
	
	public void storeApiData(long guestId, List<AbstractFacet> facet);
	
}
