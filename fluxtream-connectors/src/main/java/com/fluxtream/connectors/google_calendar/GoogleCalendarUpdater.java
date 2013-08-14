package com.fluxtream.connectors.google_calendar;

import java.io.IOException;
import java.util.List;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.SettingsAwareAbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.SettingsService;
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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Calendar", value = 0, objectTypes={GoogleCalendarEventFacet.class}, settings=GoogleCalendarConnectorSettings.class)
public class GoogleCalendarUpdater extends SettingsAwareAbstractUpdater {

    private static final FlxLogger logger = FlxLogger.getLogger(GoogleCalendarUpdater.class);

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    SettingsService settingsService;

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        // if we're coming from an older install and this user has oauth 1 keys,
        // suggest renewing the tokens in the manage connectors dialog
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerKey")!=null) {
            sendOauth2UpgradeWarning(updateInfo);
        } else
            loadHistory(updateInfo, false);
    }

    private void sendOauth2UpgradeWarning(final UpdateInfo updateInfo) {
        notificationsService.addNotification(updateInfo.getGuestId(), Notification.Type.WARNING,
                                             "Heads Up. This server has recently been upgraded to a version that supports<br>" +
                                             "oauth 2 with Google APIs. Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                             "head to the Google Calendar updater and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        // if we're coming from an older install and this user has oauth 1 keys,
        // suggest renewing the tokens in the manage connectors dialog
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerKey")!=null) {
            sendOauth2UpgradeWarning(updateInfo);
        } else
            loadHistory(updateInfo, true);
    }

    private void loadHistory(UpdateInfo updateInfo, boolean incremental) throws Exception {
        Calendar calendar = getCalendar(updateInfo.apiKey);
        String pageToken = null;
        long apiKeyId = updateInfo.apiKey.getId();
        settingsService.getConnectorSettings(updateInfo.apiKey.getId(), true);
        List<String> existingCalendarIds = getExistingCalendarIds(apiKeyId);
        do {
            final long then = System.currentTimeMillis();
            final Calendar.CalendarList.List list = calendar.calendarList().list().setPageToken(pageToken);
            final String query = list.getUriTemplate();
            CalendarList calendarList = null;
            try {
                calendarList = list.execute();
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query);
            } catch (Exception e) {
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query, ExceptionUtils.getStackTrace(e));
            }
            if (calendarList==null) throw new Exception("Could not get calendar list, apiKeyId=" + updateInfo.apiKey.getId());
            List<CalendarListEntry> items = calendarList.getItems();
            for (CalendarListEntry item : items) {
                existingCalendarIds.remove(item.getId());
                loadCalendarHistory(calendar, item, updateInfo, incremental);
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);
        deleteCalendars(apiKeyId, existingCalendarIds);
    }

    private void deleteCalendars(final long apiKeyId, final List<String> existingCalendarIds) {
        for (String existingCalendarId : existingCalendarIds) {
            final String deleteQuery = "DELETE FROM Facet_GoogleCalendarEvent WHERE apiKeyId=" + apiKeyId + " AND calendarId='" + existingCalendarId + "'";
            final long deleted = jpaDaoService.execute(deleteQuery);
            System.out.println("deleted " + deleted + " events from calendar '" + existingCalendarId + "'");
        }
    }

    private List<String> getExistingCalendarIds(long apiKeyId) {
        List<String> l = (List<String>) jpaDaoService.executeNativeQuery("SELECT distinct calendarId from Facet_GoogleCalendarEvent WHERE apiKeyId=?", apiKeyId);
        return l;
    }

    private void loadCalendarHistory(final Calendar calendar, final CalendarListEntry calendarEntry, final UpdateInfo updateInfo, final boolean incremental) throws IOException {
        if (incremental) {
            // first update existing events
            String queryString = "select max(eventUpdated) from Facet_GoogleCalendarEvent where apiKeyId=" + updateInfo.apiKey.getId() + " and calendarId='" + calendarEntry.getId() + "'";
            Long since = jpaDaoService.executeNativeQuery(queryString);
            if (since!=null)
                updateCalendarEvents(calendar, calendarEntry, updateInfo, since);
            // now fetch new events
            queryString = "select max(start) from Facet_GoogleCalendarEvent where apiKeyId=" + updateInfo.apiKey.getId() + " and calendarId='" + calendarEntry.getId() + "'";
            since = jpaDaoService.executeNativeQuery(queryString);
            if (since!=null)
                loadCalendarEvents(calendar, calendarEntry, updateInfo, since);
        } else {
            loadCalendarEvents(calendar, calendarEntry, updateInfo, null);
        }
    }

    private void updateCalendarEvents(final Calendar calendar,
                                    final CalendarListEntry calendarEntry,
                                    final UpdateInfo updateInfo,
                                    final long since) throws IOException {
        String pageToken = null;
        do {
            long then = System.currentTimeMillis();
            final Calendar.Events.List eventsApiCall = calendar.events().list(calendarEntry.getId());
            final String uriTemplate = eventsApiCall.getUriTemplate();
            try {
                eventsApiCall.setPageToken(pageToken);
                eventsApiCall.setShowHiddenInvitations(true);
                eventsApiCall.setSingleEvents(true);
                eventsApiCall.setTimeMax(new DateTime(System.currentTimeMillis()));
                eventsApiCall.setUpdatedMin(new DateTime(since));
                final Events events = eventsApiCall.execute();
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, uriTemplate);
                final List<Event> eventList = events.getItems();
                storeEvents(updateInfo, calendarEntry, eventList);
                pageToken = events.getNextPageToken();
            } catch (Throwable e) {
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, uriTemplate, ExceptionUtils.getStackTrace(e));
                throw(new RuntimeException(e));
            }
        } while (pageToken != null);
    }

    private void loadCalendarEvents(final Calendar calendar,
                                    final CalendarListEntry calendarEntry,
                                    final UpdateInfo updateInfo,
                                    final Long since) throws IOException {
        String pageToken = null;
        do {
            long then = System.currentTimeMillis();
            final Calendar.Events.List eventsApiCall = calendar.events().list(calendarEntry.getId());
            final String uriTemplate = eventsApiCall.getUriTemplate();
            try {
                eventsApiCall.setPageToken(pageToken);
                eventsApiCall.setShowHiddenInvitations(true);
                eventsApiCall.setSingleEvents(true);
                eventsApiCall.setTimeMax(new DateTime(System.currentTimeMillis()));
                if (since!=null)
                    eventsApiCall.setTimeMin(new DateTime(since));
                final Events events = eventsApiCall.execute();
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, uriTemplate);
                final List<Event> eventList = events.getItems();
                storeEvents(updateInfo, calendarEntry, eventList);
                pageToken = events.getNextPageToken();
            } catch (Throwable e) {
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, uriTemplate, ExceptionUtils.getStackTrace(e));
                throw(new RuntimeException(e));
            }
        } while (pageToken != null);
    }

    private void storeEvents(final UpdateInfo updateInfo, final CalendarListEntry calendarEntry, final List<Event> eventList) {
        for (final Event event : eventList) {
            createOrUpdateEvent(updateInfo, calendarEntry, event);
        }
    }

    private void createOrUpdateEvent(final UpdateInfo updateInfo, final CalendarListEntry calendarEntry, final Event event) {
        if (event.getStatus().equalsIgnoreCase("cancelled")) {
            System.out.println("event " + event.getSummary() + "/" + event.getDescription() + " was canceled");
            final int deleted = jpaDaoService.execute(String.format("DELETE FROM Facet_GoogleCalendarEvent facet WHERE " +
                                                                    "facet.apiKeyId=%s AND facet.googleId='%s'",
                                                                    updateInfo.apiKey.getId(), event.getId()));
            System.out.println("deleted " + deleted + " calendar entry");
            return;
        }
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.googleId=? AND e.calendarId=?",
                                                                                   updateInfo.apiKey.getId(), event.getId(), calendarEntry.getId());
        final ApiDataService.FacetModifier<GoogleCalendarEventFacet> facetModifier = new ApiDataService.FacetModifier<GoogleCalendarEventFacet>() {
            @Override
            public GoogleCalendarEventFacet createOrModify(GoogleCalendarEventFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new GoogleCalendarEventFacet(updateInfo.apiKey.getId());
                    facet.googleId = event.getId();
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                    facet.calendarId = calendarEntry.getId();
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
                facet.transparency = event.getTransparency();
                facet.visibility = event.getVisibility();
                facet.setRecurrence(event.getRecurrence());
                facet.recurringEventId = event.getRecurringEventId();
                facet.sequence = event.getSequence();
                facet.setUpdated(event.getUpdated());
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(GoogleCalendarEventFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    @Override
    public Object createOrRefreshSettings(final ApiKey apiKey) {
        GoogleCalendarConnectorSettings settings = (GoogleCalendarConnectorSettings)apiKey.getSettings();
        if (settings==null)
            settings = new GoogleCalendarConnectorSettings();
        refreshSettings(apiKey, settings);
        return settings;
    }

    private void refreshSettings(final ApiKey apiKey, final GoogleCalendarConnectorSettings settings) {
        final Calendar calendar = getCalendar(apiKey);
        final Calendar.CalendarList calendarList = calendar.calendarList();
        final long then = System.currentTimeMillis();
        try {
            final Calendar.CalendarList.List calendarListCall = calendarList.list();
            final String uriTemplate = calendarListCall.getUriTemplate();
            CalendarList list = null;
            try {
                list = calendarListCall.execute();
                countSuccessfulApiCall(apiKey, 0xffffff, then, uriTemplate);
            } catch (Throwable t) {
                countFailedApiCall(apiKey, 0xffffff, then, uriTemplate, ExceptionUtils.getStackTrace(t));
            }
            final List<CalendarListEntry> items = list.getItems();
            for (CalendarListEntry calendarListEntry : items) {
                final String calendarId = calendarListEntry.getId();
                CalendarConfig config = settings.getCalendar(calendarId);
                if (config==null) {
                    config = new CalendarConfig();
                    config.id = calendarId;
                    settings.addCalendarConfig(config);
                }
                config.foregroundColor = calendarListEntry.getForegroundColor();
                config.backgroundColor = calendarListEntry.getBackgroundColor();
                config.summary = calendarListEntry.getSummary();
                config.summaryOverride = calendarListEntry.getSummaryOverride();
                config.description = calendarListEntry.getDescription();
                config.primary = calendarListEntry.getPrimary()!=null?calendarListEntry.getPrimary():false;
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
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
