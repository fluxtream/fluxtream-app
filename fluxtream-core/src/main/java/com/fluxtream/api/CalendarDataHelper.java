package com.fluxtream.api;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.AbstractRepeatableFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.metadata.AbstractTimespanMetadata;
import com.fluxtream.metadata.DayMetadata;
import com.fluxtream.mvc.models.TimeBoundariesModel;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.CoachingService;
import com.fluxtream.services.GuestService;
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
            if (AuthHelper.isViewingGranted(connector.getName(), coachingService))
                facets = apiDataService.getApiDataFacets(
                        guestService.getApiKey(AuthHelper.getVieweeId(), connector), objectType,
                        dates);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return facets;
    }

	public List<AbstractFacet> getFacets(Connector connector,
			ObjectType objectType, AbstractTimespanMetadata timespanMetadata) {
		List<AbstractFacet> facets = new ArrayList<AbstractFacet>();
		try {
            if (AuthHelper.isViewingGranted(connector.getName(), coachingService))
                facets = apiDataService.getApiDataFacets(guestService.getApiKey(AuthHelper.getVieweeId(), connector), objectType, timespanMetadata.getTimeInterval());
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
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return facets;
    }
}
