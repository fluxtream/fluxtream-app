package org.fluxtream.services;

import java.util.List;

import org.fluxtream.domain.AbstractFacet;

public interface FullTextSearchService {

	public void reinitializeIndex() throws Exception;
	
	public List<AbstractFacet> searchFacetsIndex(long guestId, String terms) throws Exception;

}
