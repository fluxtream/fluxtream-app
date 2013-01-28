package com.fluxtream.connectors.google_calendar;

import java.net.URL;
import java.util.List;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractGoogleOAuthUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.utils.Utils;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.extensions.When;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Calendar", value = 0, updateStrategyType = UpdateStrategyType.ALWAYS_UPDATE, objectTypes = {GoogleCalendarEntryFacet.class})
@JsonFacetCollection(GoogleCalendarFacetVOCollection.class)
public class GoogleCalendarUpdater extends AbstractGoogleOAuthUpdater {

    private static final Logger LOG = Logger.getLogger(GoogleCalendarUpdater.class);

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        loadHistory(updateInfo, 0, System.currentTimeMillis());
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        loadHistory(updateInfo,
                    updateInfo.timeInterval.start,
                    updateInfo.timeInterval.end);
    }

    private void loadHistory(UpdateInfo updateInfo, long from, long to) throws Exception {
        long then = System.currentTimeMillis();
        String requestUrl = "request url not set yet";
        List<CalendarEventEntry> entries;
        try {
            URL feedUrl = new URL("https://www.google.com/calendar/feeds/default/private/full");
            CalendarQuery myQuery = new CalendarQuery(feedUrl);
            myQuery.setStringCustomParameter("singleevents", "true");   // See http://stackoverflow.com/a/3648754
            myQuery.setMaxResults(200);
            myQuery.setMinimumStartTime(DateTime.parseDateTime(dateFormat.print(from)));
            myQuery.setMaximumStartTime(DateTime.parseDateTime(dateFormat.print(to)));

            requestUrl = myQuery.getFeedUrl().getQuery();

            CalendarService myService = new CalendarService("");
            myService.setOAuthCredentials(getOAuthParameters(updateInfo.apiKey),
                                          new OAuthHmacSha1Signer());

            CalendarEventFeed resultFeed = myService.query(myQuery,
                                                           CalendarEventFeed.class);

            entries = resultFeed.getEntries();
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes,
                               then,
                               requestUrl, Utils.stackTrace(e));
            throw e;
        }

        countSuccessfulApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes,
                               then,
                               requestUrl);

        if (entries != null) {
            for (CalendarEventEntry calendarEventEntry : entries) {
                final List<When> times = calendarEventEntry.getTimes();
                if (times != null) {
                    if (!times.isEmpty()) {
                        GoogleCalendarEntryFacet payload = new GoogleCalendarEntryFacet(calendarEventEntry, updateInfo.apiKey.getId());
                        payload.start = times.get(0).getStartTime().getValue();
                        payload.end = times.get(0).getEndTime().getValue();
                        apiDataService.cacheApiDataObject(updateInfo, -1, -1, payload);
                    }
                    else {
                        if (LOG.isEnabledFor(Level.ERROR)) {
                            LOG.error("GoogleCalendarUpdater.loadHistory(): CalendarEventEntry times is empty for event [" + calendarEventEntry.getTitle().getPlainText() + "]");
                        }
                    }
                }
                else {
                    if (LOG.isEnabledFor(Level.ERROR)) {
                        LOG.error("GoogleCalendarUpdater.loadHistory(): CalendarEventEntry times is null for event [" + calendarEventEntry.getTitle().getPlainText());
                    }
                }
            }
        }
        else {
            throw new Exception("null entries when loading google calendar history");
        }
    }
}
