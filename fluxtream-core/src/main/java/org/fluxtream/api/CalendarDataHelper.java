package org.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import org.fluxtream.auth.AuthHelper;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.AbstractRepeatableFacet;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.metadata.AbstractTimespanMetadata;
import org.fluxtream.metadata.DayMetadata;
import org.fluxtream.mvc.models.TimeBoundariesModel;
import org.fluxtream.services.ApiDataService;
import org.fluxtream.services.CoachingService;
import org.fluxtream.services.GuestService;
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
	TimeBoundariesModel getStartEndResponseBoundaries(DayMetadata dayMetadata) {
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
