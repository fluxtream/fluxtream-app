package org.fluxtream.core.api;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractRepeatableFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.metadata.AbstractTimespanMetadata;
import org.fluxtream.core.mvc.models.TimeBoundariesModel;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.CoachingService;
import org.fluxtream.core.services.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CalendarDataHelper {

	@Autowired
	private ApiDataService apiDataService;

    @Autowired
    private GuestService guestService;

    @Autowired
    private CoachingService coachingService;

	/**
	 * This is to let the client discard responses that are coming "too late"
	 * 
	 */
	TimeBoundariesModel getStartEndResponseBoundaries(AbstractTimespanMetadata dayMetadata) {
		TimeBoundariesModel tb = new TimeBoundariesModel();
		tb.start = dayMetadata.start;
		tb.end = dayMetadata.end;
		return tb;
	}

    public List<AbstractFacet> getFacets(Connector connector,
                                         ObjectType objectType,
                                         List<String> dates) {
        List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
        try {
            if (AuthHelper.isViewingGranted(connector.getName(), coachingService)) {
                final ApiKey apiKey = guestService.getApiKey(AuthHelper.getVieweeId(), connector);
                facets = apiDataService.getApiDataFacets(apiKey, objectType,
                        dates);
                facets = coachingService.filterFacets(AuthHelper.getGuestId(), apiKey.getId(), facets);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return facets;
    }

	public List<AbstractFacet> getFacets(Connector connector,
			ObjectType objectType, AbstractTimespanMetadata timespanMetadata) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
		try {
            if (AuthHelper.isViewingGranted(connector.getName(), coachingService)) {
                final ApiKey apiKey = guestService.getApiKey(AuthHelper.getVieweeId(), connector);
                facets = apiDataService.getApiDataFacets(apiKey, objectType, timespanMetadata.getTimeInterval());
                facets = coachingService.filterFacets(AuthHelper.getGuestId(), apiKey.getId(), facets);
            }
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return facets;
	}

    public List<AbstractRepeatableFacet> getFacets(final Connector connector, final ObjectType objectType, final String startDate, final String endDate) {
        List<AbstractRepeatableFacet> facets = new ArrayList<AbstractRepeatableFacet>();
        try {
            if (AuthHelper.isViewingGranted(connector.getName(), coachingService)) {
                final ApiKey apiKey = guestService.getApiKey(AuthHelper.getVieweeId(), connector);
                facets = apiDataService.getApiDataFacets(apiKey, objectType,
                        startDate, endDate);
                facets = coachingService.filterFacets(AuthHelper.getGuestId(), apiKey.getId(), facets);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return facets;
    }
}
