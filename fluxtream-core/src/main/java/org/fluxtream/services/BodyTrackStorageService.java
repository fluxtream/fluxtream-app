package org.fluxtream.services;

import java.util.List;

import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.ApiKey;

public interface BodyTrackStorageService {

	public void storeInitialHistory(ApiKey apiKey);
	
	public void storeApiData(long guestId, List<AbstractFacet> facet);
	
}
