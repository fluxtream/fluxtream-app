package org.fluxtream.facets.extractors;

import java.util.List;
import org.fluxtream.ApiData;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;

public abstract class AbstractFacetExtractor {

	protected Connector connector(UpdateInfo updateInfo) {
		return updateInfo.apiKey.getConnector();
	}

	protected void extractCommonFacetData(AbstractFacet facet, ApiData apiData) {
        facet.apiKeyId = apiData.updateInfo.apiKey.getId();
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

    protected String noon(String date) {
        return date + "T12:00:00.000";
    }

    protected String toTimeStorage(int year, int month, int day, int hours,
                                   int minutes, int seconds) {
        //yyyy-MM-dd'T'HH:mm:ss.SSS
        return (new StringBuilder()).append(year)
                .append("-").append(pad(month)).append("-")
                .append(pad(day)).append("T").append(pad(hours))
                .append(":").append(pad(minutes)).append(":")
                .append(pad(seconds)).append(".000").toString();
    }

    protected static String pad(int i) {
        return i<10
               ? (new StringBuilder("0").append(i)).toString()
               : String.valueOf(i);
    }

	public abstract List<AbstractFacet> extractFacets(UpdateInfo updateInfo, ApiData apiData,
			ObjectType objectType) throws Exception;
}
