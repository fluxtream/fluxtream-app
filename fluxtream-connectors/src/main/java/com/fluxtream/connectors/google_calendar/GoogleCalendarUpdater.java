package com.fluxtream.connectors.google_calendar;

import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Calendar", value = 0, updateStrategyType = UpdateStrategyType.ALWAYS_UPDATE, objectTypes={})
public class GoogleCalendarUpdater extends AbstractUpdater {

    private static final FlxLogger logger = FlxLogger.getLogger(GoogleCalendarUpdater.class);

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        loadHistory(updateInfo, 0, System.currentTimeMillis());
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        if (updateInfo.timeInterval==null) {
            logger.info("module=updateQueue component=googleCalendarUpdater action=updateConnectorData message=\"attempt to update connector data with a null timeInterval\"");
        } else
            loadHistory(updateInfo, updateInfo.timeInterval.getStart(), updateInfo.timeInterval.getEnd());
    }

    private void loadHistory(UpdateInfo updateInfo, long from, long to) throws Exception {
        //long then = System.currentTimeMillis();
        //String requestUrl = "request url not set yet";
        //List<CalendarEventEntry> entries;
        //try {
        //    URL feedUrl = new URL("https://www.google.com/calendar/feeds/default/private/full");
        //    CalendarQuery myQuery = new CalendarQuery(feedUrl);
        //    myQuery.setStringCustomParameter("singleevents", "true");   // See http://stackoverflow.com/a/3648754
        //    myQuery.setMaxResults(200);
        //    myQuery.setMinimumStartTime(DateTime.parseDateTime(dateFormat.print(from)));
        //    myQuery.setMaximumStartTime(DateTime.parseDateTime(dateFormat.print(to)));
        //
        //    requestUrl = myQuery.getFeedUrl().getQuery();
        //
        //    CalendarService myService = new CalendarService("");
        //    myService.setOAuthCredentials(getOAuthParameters(updateInfo.apiKey),
        //                                  new OAuthHmacSha1Signer());
        //
        //    CalendarEventFeed resultFeed = myService.query(myQuery,
        //                                                   CalendarEventFeed.class);
        //
        //    entries = resultFeed.getEntries();
        //}
        //catch (Exception e) {
        //    countFailedApiCall(updateInfo.apiKey,
        //                       updateInfo.objectTypes,
        //                       then,
        //                       requestUrl, Utils.stackTrace(e));
        //    throw e;
        //}

        //countSuccessfulApiCall(updateInfo.apiKey,
        //                       updateInfo.objectTypes,
        //                       then,
        //                       requestUrl);
        //
        //if (entries != null) {
        //    for (CalendarEventEntry calendarEventEntry : entries) {
        //        final List<When> times = calendarEventEntry.getTimes();
        //        if (times != null) {
        //            if (!times.isEmpty()) {
        //                GoogleCalendarEntryFacet payload = new GoogleCalendarEntryFacet(calendarEventEntry, updateInfo.apiKey.getId());
        //                payload.start = times.get(0).getStartTime().getValue();
        //                payload.end = times.get(0).getEndTime().getValue();
        //                apiDataService.cacheApiDataObject(updateInfo, -1, -1, payload);
        //            }
        //            else {
        //                if (logger.isEnabledFor(Level.ERROR)) {
        //                    logger.error("GoogleCalendarUpdater.loadHistory(): CalendarEventEntry times is empty for event [" + calendarEventEntry.getTitle().getPlainText() + "]");
        //                }
        //            }
        //        }
        //        else {
        //            if (logger.isEnabledFor(Level.ERROR)) {
        //                logger.error("GoogleCalendarUpdater.loadHistory(): CalendarEventEntry times is null for event [" + calendarEventEntry.getTitle().getPlainText());
        //            }
        //        }
        //    }
        //}
        //else {
        //    throw new Exception("null entries when loading google calendar history");
        //}
    }
}
