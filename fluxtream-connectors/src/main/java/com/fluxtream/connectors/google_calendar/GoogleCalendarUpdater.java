package com.fluxtream.connectors.google_calendar;

import java.io.IOException;
import java.util.List;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.SettingsAwareAbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ApiDataService;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Calendar", value = 0, objectTypes={GoogleCalendarEventFacet.class})
public class GoogleCalendarUpdater extends SettingsAwareAbstractUpdater {

    private static final FlxLogger logger = FlxLogger.getLogger(GoogleCalendarUpdater.class);

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        loadHistory(updateInfo, 0);
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        if (updateInfo.timeInterval==null) {
            logger.info("module=updateQueue component=googleCalendarUpdater action=updateConnectorData message=\"attempt to update connector data with a null timeInterval\"");
        } else
            loadHistory(updateInfo, updateInfo.timeInterval.getStart());
    }

    private void loadHistory(UpdateInfo updateInfo, long from) throws Exception {
        Calendar calendar = getCalendar(updateInfo.apiKey);
        final Calendar.CalendarList calendarList = calendar.calendarList();
        try {
            final CalendarList list = calendarList.list().execute();
            final List<CalendarListEntry> items = list.getItems();
            for (CalendarListEntry item : items) {
                final String calendarId = item.getId();
                loadCalendarHistory(calendar, calendarId, updateInfo, from);
            }
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void loadCalendarHistory(final Calendar calendar, final String calendarId, final UpdateInfo updateInfo, final long from) throws IOException {
        String pageToken = null;
        do {
            final Calendar.Events.List eventsApiCall = calendar.events().list(calendarId);
            eventsApiCall.setPageToken(pageToken);
            if (from==0) {
                eventsApiCall.setTimeMin(new DateTime(from));
                eventsApiCall.setTimeMax(new DateTime(System.currentTimeMillis()));
            } else {
                eventsApiCall.setUpdatedMin(new DateTime(from));
            }
            final Events events = eventsApiCall.execute();
            final List<Event> eventList = events.getItems();
            storeEvents(updateInfo, eventList);
        } while (pageToken != null);
    }

    private void storeEvents(final UpdateInfo updateInfo, final List<Event> eventList) {
        for (final Event event : eventList) {
            createOrUpdateEvent(updateInfo, event);
        }
    }

    private void createOrUpdateEvent(final UpdateInfo updateInfo, final Event event) {
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.googleId=?", updateInfo.apiKey.getId(), event.getId());
        final ApiDataService.FacetModifier<GoogleCalendarEventFacet> facetModifier = new ApiDataService.FacetModifier<GoogleCalendarEventFacet>() {
            @Override
            public GoogleCalendarEventFacet createOrModify(GoogleCalendarEventFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new GoogleCalendarEventFacet(updateInfo.apiKey.getId());
                    facet.googleId = event.getId();
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                }
                facet.summary = event.getSummary();
                facet.setCreated(event.getCreated());
                facet.setAttendees(event.getAttendees());
                facet.etag = event.getEtag();
                facet.setStart(event.getStart());
                facet.endTimeUnspecified = event.getEndTimeUnspecified();
                facet.setEnd(event.getEnd());
                facet.colorId = event.getColorId();
                facet.setCreator(event.getCreator());
                facet.description = event.getDescription();
                facet.guestsCanSeeOtherGuests = event.getGuestsCanSeeOtherGuests();
                facet.hangoutLink = event.getHangoutLink();
                facet.htmlLink = event.getHtmlLink();
                facet.iCalUID = event.getICalUID();
                facet.kind = event.getKind();
                facet.location = event.getLocation();
                facet.locked = event.getLocked();
                facet.setOrganizer(event.getOrganizer());
                facet.setOriginalStartTime(event.getOriginalStartTime());
                facet.status = event.getStatus();
                facet.timeUpdated = System.currentTimeMillis();
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(GoogleCalendarEventFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    @Override
    public Object getSettings(final ApiKey apiKey) {
        GoogleCalendarConnectorSettings settings = (GoogleCalendarConnectorSettings)apiKey.getSettings();
        if (settings==null)
            settings = new GoogleCalendarConnectorSettings();
        updateSettings(apiKey, settings);
        return settings;
    }

    private void updateSettings(final ApiKey apiKey, final GoogleCalendarConnectorSettings settings) {
        final Calendar calendar = getCalendar(apiKey);
        final Calendar.CalendarList calendarList = calendar.calendarList();
        try {
            final CalendarList list = calendarList.list().execute();
            final List<CalendarListEntry> items = list.getItems();
            System.out.println(items);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Calendar getCalendar(final ApiKey apiKey) {
        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        final String accessToken = guestService.getApiKeyAttribute(apiKey, "accessToken");
        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        final String clientId = env.get("google.client.id");
        final String clientSecret = env.get("google.client.secret");
        final GoogleCredential.Builder builder = new GoogleCredential.Builder();
        builder.setTransport(httpTransport);
        builder.setJsonFactory(jsonFactory);
        builder.setClientSecrets(clientId, clientSecret);
        GoogleCredential credential = builder.build();
        final Long tokenExpires = Long.valueOf(guestService.getApiKeyAttribute(apiKey, "tokenExpires"));
        credential.setExpirationTimeMilliseconds(tokenExpires);
        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);
        try {
            if (tokenExpires<System.currentTimeMillis()) {
                boolean tokenRefreshed = credential.refreshToken();
                logger.info("google calendar token has been refreshed: " + tokenRefreshed);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Calendar.Builder calendarBuilder = new Calendar.Builder(httpTransport, jsonFactory, credential);
        final Calendar calendar = calendarBuilder.build();
        return calendar;
    }

}
