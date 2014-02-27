package com.fluxtream.connectors.google_calendar;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.connectors.SharedConnectorFilter;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.SharedConnector;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * User: candide
 * Date: 27/02/14
 * Time: 17:18
 */
public class GoogleCalendarSharedConnectorFilter implements SharedConnectorFilter {

    @Override
    public <T extends AbstractFacet> List<T> filterFacets(final SharedConnector sharedConnector, final List<T> facets) {
        if (sharedConnector.filterJson==null)
            return facets;
        JSONObject json = JSONObject.fromObject(sharedConnector.filterJson);
        final JSONArray calendars = json.getJSONArray("calendars");
        List<String> sharedCalendarIds = new ArrayList<String>();
        for (int i=0; i<calendars.size(); i++) {
            JSONObject calendar = calendars.getJSONObject(i);
            boolean shared = calendar.getBoolean("shared");
            if (shared)
                sharedCalendarIds.add(calendar.getString("guid"));
        }
        List<T> filteredFacets = new ArrayList<T>();
        for (T facet : facets) {
            if (facet instanceof GoogleCalendarEventFacet) {
                if(sharedCalendarIds.contains(((GoogleCalendarEventFacet)facet).calendarId))
                    filteredFacets.add(facet);
            }
        }
        return filteredFacets;
    }

}
