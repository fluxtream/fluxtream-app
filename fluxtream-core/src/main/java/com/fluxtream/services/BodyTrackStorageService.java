package com.fluxtream.services;

import java.util.List;

import com.fluxtream.domain.AbstractFacet;

public interface BodyTrackStorageService {

	public void storeInitialHistory(long guestId, String connectorName);
	
	public void storeApiData(long guestId, List<AbstractFacet> facet);
	
}
