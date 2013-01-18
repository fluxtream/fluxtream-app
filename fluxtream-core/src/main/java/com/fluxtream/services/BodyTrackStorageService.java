package com.fluxtream.services;

import java.util.List;

import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;

public interface BodyTrackStorageService {

	public void storeInitialHistory(ApiKey apiKey);
	
	public void storeApiData(long guestId, List<AbstractFacet> facet);
	
}
