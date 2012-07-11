package com.fluxtream.facets.extractors;

import java.util.List;

import com.fluxtream.ApiData;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public abstract class AbstractFacetExtractor {

    protected final static DateTimeFormatter dateFormatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    protected UpdateInfo updateInfo;

	public void setUpdateInfo(UpdateInfo updateInfo) {
		this.updateInfo = updateInfo;
	}
	
	protected Connector connector() {
		return updateInfo.apiKey.getConnector();
	}

	protected void extractCommonFacetData(AbstractFacet facet, ApiData apiData) {
		facet.guestId = apiData.updateInfo.apiKey.getGuestId();
		facet.api = apiData.updateInfo.apiKey.getConnector().value();
		facet.timeUpdated = System.currentTimeMillis();
		// may be overridden by subclasses, this is just a "first approximation"
		// that may of may not be provided by the specialized extractor
		if (apiData.start!=-1)
			facet.start = apiData.start;
		if (apiData.end!=-1)
			facet.end = apiData.end;
	}

	public abstract List<AbstractFacet> extractFacets(ApiData apiData,
			ObjectType objectType) throws Exception;
}
