package com.fluxtream.connectors.google_calendar;

import java.net.URL;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractGoogleOAuthUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;

@Component
@Updater(prettyName = "Google Calendar", value = 0, updateStrategyType = UpdateStrategyType.ALWAYS_UPDATE, objectTypes = { GoogleCalendarEntryFacet.class })
@JsonFacetCollection(GoogleCalendarFacetVOCollection.class)
public class GoogleCalendarUpdater extends AbstractGoogleOAuthUpdater {

	public GoogleCalendarUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		loadHistory(updateInfo, 0, System.currentTimeMillis());
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		loadHistory(updateInfo, updateInfo.timeInterval.start,
				updateInfo.timeInterval.end);
	}

	private void loadHistory(UpdateInfo updateInfo, long from, long to)
			throws Exception {
		long then = System.currentTimeMillis();
		String requestUrl = "request url not set yet";
		List<CalendarEventEntry> entries = null;
		try {
			URL feedUrl = new URL(
					"https://www.google.com/calendar/feeds/default/private/full");
			CalendarQuery myQuery = new CalendarQuery(feedUrl);
			myQuery.setMaxResults(200);
			myQuery.setMinimumStartTime(DateTime.parseDateTime(dateFormat
					.print(from)));
			myQuery.setMaximumStartTime(DateTime.parseDateTime(dateFormat
					.print(to)));

			requestUrl = myQuery.getFeedUrl().getQuery();

			CalendarService myService = new CalendarService("fluxtream");
			myService.setOAuthCredentials(
					getOAuthParameters(updateInfo.apiKey),
					new OAuthHmacSha1Signer());

			CalendarEventFeed resultFeed = myService.query(myQuery,
					CalendarEventFeed.class);

			entries = resultFeed.getEntries();
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, requestUrl);
			throw e;
		}

		countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
				updateInfo.objectTypes, then, requestUrl);

		if (entries != null) {
			for (CalendarEventEntry calendarEventEntry : entries) {
				GoogleCalendarEntryFacet payload = new GoogleCalendarEntryFacet(
						calendarEventEntry);
				payload.start = calendarEventEntry.getTimes().get(0)
						.getStartTime().getValue();
				payload.end = calendarEventEntry.getTimes().get(0).getEndTime()
						.getValue();
				apiDataService.cacheApiDataObject(updateInfo, -1, -1,
						payload);
			}
		} else
			throw new Exception(
					"null entries when loading google calendar history");
	}

}
