package org.fluxtream.connectors.google_calendar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.SettingsAwareUpdater;
import org.fluxtream.connectors.updaters.SharedConnectorSettingsAwareUpdater;
import org.fluxtream.connectors.updaters.UpdateFailedException;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.ApiKey;
import org.fluxtream.domain.ChannelMapping;
import org.fluxtream.domain.Notification;
import org.fluxtream.domain.SharedConnector;
import org.fluxtream.services.ApiDataService;
import org.fluxtream.services.CoachingService;
import org.fluxtream.services.JPADaoService;
import org.fluxtream.services.SettingsService;
import org.fluxtream.services.impl.BodyTrackHelper;
import org.fluxtream.utils.Utils;
import com.google.api.client.auth.oauth2.TokenResponseException;
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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.fluxtream.utils.Utils.hash;

@Component
@Updater(prettyName = "Calendar", value = 0, objectTypes={GoogleCalendarEventFacet.class},
         settings=GoogleCalendarConnectorSettings.class, bodytrackResponder = GoogleCalendarBodytrackResponder.class,
         defaultChannels = {"google_calendar.events"},
         sharedConnectorFilter = GoogleCalendarSharedConnectorFilter.class)
public class GoogleCalendarUpdater extends AbstractUpdater implements SettingsAwareUpdater, SharedConnectorSettingsAwareUpdater {

    private static final FlxLogger logger = FlxLogger.getLogger(GoogleCalendarUpdater.class);
    private final String LAST_TIME_WE_CHECKED_FOR_UPDATED_EVENTS_ATT = "lastTimeWeCheckedForUpdatedEvents";
    private final String REMOTE_CALLENDARS_KEY = "remoteCalendars";

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    CoachingService coachingService;

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception, UpdateFailedException {
        // if we're coming from an older install and this user has oauth 1 keys,
        // suggest renewing the tokens in the manage connectors dialog
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerKey")!=null||
                    guestService.getApiKeyAttribute(updateInfo.apiKey, "refreshToken")==null) {
            sendOauth2UpgradeWarning(updateInfo);
        } else {
            // Store a conservative value for the last time we checked for updated events so that we
            // don't miss updates that happen between the start of the check and when the check completes
            String lastTimeCheckedForUpdatedEvents = String.valueOf(System.currentTimeMillis());
            loadHistory(updateInfo, false);
            guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_TIME_WE_CHECKED_FOR_UPDATED_EVENTS_ATT, lastTimeCheckedForUpdatedEvents);
        }
    }

    private void sendOauth2UpgradeWarning(final UpdateInfo updateInfo) throws UpdateFailedException {
        notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                  "Heads Up. This server has recently been upgraded to a version that supports<br>" +
                                                  "oauth 2 with Google APIs. Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                  "scroll to the Google Calendar connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");
        // Report this connector as having failed permanently
        throw new UpdateFailedException("requires token reauthorization", true);
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception, UpdateFailedException {
        // if we're coming from an older install and this user has oauth 1 keys,
        // suggest renewing the tokens in the manage connectors dialog
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "googleConsumerKey")!=null||
            guestService.getApiKeyAttribute(updateInfo.apiKey, "refreshToken")==null) {
            sendOauth2UpgradeWarning(updateInfo);
        } else {
            loadHistory(updateInfo, true);
        }
    }

    @Override
    public void connectorSettingsChanged(final long apiKeyId, final Object settings) {
        final GoogleCalendarConnectorSettings connectorSettings = (GoogleCalendarConnectorSettings)settings;
        final ApiKey apiKey = guestService.getApiKey(apiKeyId);
        initChannelMapping(apiKey, connectorSettings.calendars);
    }

    private void initChannelMapping(ApiKey apiKey, final List<CalendarConfig> calendarConfigs) {
        bodyTrackHelper.deleteChannelMappings(apiKey);
        ChannelMapping mapping = new ChannelMapping();
        mapping.deviceName = "google_calendar";
        mapping.channelName = "events";
        mapping.timeType = ChannelMapping.TimeType.gmt;
        mapping.channelType = ChannelMapping.ChannelType.timespan;
        mapping.guestId = apiKey.getGuestId();
        mapping.apiKeyId = apiKey.getId();
        bodyTrackHelper.persistChannelMapping(mapping);

        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#92da46";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#92da46";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
        channelStyle.timespanStyles.defaultStyle.top = 1.0;
        channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
        channelStyle.timespanStyles.values = new HashMap();

        GoogleCalendarConnectorSettings connectorSettings =
                (GoogleCalendarConnectorSettings)settingsService.getConnectorSettings(apiKey.getId());
        int n = calendarConfigs.size();
        if (connectorSettings!=null) {
            n = 0;
            for (CalendarConfig calendar : connectorSettings.calendars) {
                if (!calendar.hidden)
                    n++;
            }
        }
        double rowHeight = 1.f/(n *2+1);
        int i=0;
        for (CalendarConfig config: calendarConfigs) {
            if (connectorSettings!=null && config.hidden)
                continue;

            BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();

            final int rowsFromTop = (i+1) * 2 - 1;

            stylePart.top = (double)rowsFromTop*rowHeight-(rowHeight*0.25);
            stylePart.bottom = stylePart.top+rowHeight+(rowHeight*0.25);
            stylePart.fillColor = config.backgroundColor;
            stylePart.borderColor = config.backgroundColor;
            channelStyle.timespanStyles.values.put(config.id, stylePart);
            i++;
        }

        bodyTrackHelper.setDefaultStyle(apiKey.getGuestId(), "google_calendar", "events", channelStyle);
    }

    private void loadHistory(UpdateInfo updateInfo, boolean incremental) throws Exception {
        Calendar calendar = getCalendar(updateInfo.apiKey);
        String pageToken = null;
        long apiKeyId = updateInfo.apiKey.getId();
        settingsService.getConnectorSettings(updateInfo.apiKey.getId());
        List<String> existingCalendarIds = getExistingCalendarIds(apiKeyId);
        List<CalendarListEntry> remoteCalendars = new ArrayList<CalendarListEntry>();
        List<CalendarConfig> configs = new ArrayList<CalendarConfig>();
        do {
            final long then = System.currentTimeMillis();
            final Calendar.CalendarList.List list = calendar.calendarList().list().setPageToken(pageToken);
            final String query = list.getUriTemplate();
            CalendarList calendarList = null;
            try {
                calendarList = list.execute();
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query);
            } catch (IOException e) {
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query, ExceptionUtils.getStackTrace(e),
                                   list.getLastStatusCode(), list.getLastStatusMessage());
            }
            if (calendarList==null) throw new Exception("Could not get calendar list, apiKeyId=" + updateInfo.apiKey.getId());
            List<CalendarListEntry> items = calendarList.getItems();
            for (CalendarListEntry item : items) {
                existingCalendarIds.remove(item.getId());
                remoteCalendars.add(item);
                configs.add(entry2Config(item));
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);
        initChannelMapping(updateInfo.apiKey, configs);

        updateInfo.setContext(REMOTE_CALLENDARS_KEY, remoteCalendars);

        for (CalendarListEntry remoteCalendar : remoteCalendars)
            loadCalendarHistory(calendar, remoteCalendar, updateInfo, incremental);
        deleteCalendars(apiKeyId, existingCalendarIds);
    }

    private CalendarConfig entry2Config(CalendarListEntry entry) {
        CalendarConfig config = new CalendarConfig();
        config.id = entry.getId();
        config.foregroundColor = entry.getForegroundColor();
        config.backgroundColor = entry.getBackgroundColor();
        config.summary = entry.getSummary();
        config.summaryOverride = entry.getSummaryOverride();
        config.description = entry.getDescription();
        config.primary = entry.getPrimary()!=null?entry.getPrimary():false;
        return config;
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
            // retrieve the last time we checked for updated events; if that attribute is null because the user had added the
            // connector prior to this commit, we look up to the maximum allowed number of days in the past (20)
            final String lastTimeString = guestService.getApiKeyAttribute(updateInfo.apiKey, LAST_TIME_WE_CHECKED_FOR_UPDATED_EVENTS_ATT);
            Long since;
            if (lastTimeString==null)
                since = System.currentTimeMillis()-20*DateTimeConstants.MILLIS_PER_DAY;
            else
                since = Long.valueOf(lastTimeString);

            // Store a conservative value for the last time we checked for updated events so that we
            // don't miss updates that happen between the start of the check and when the check completes
            String lastTimeCheckedForUpdatedEvents = String.valueOf(System.currentTimeMillis());
            updateCalendarEvents(calendar, calendarEntry, updateInfo, since);
            guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_TIME_WE_CHECKED_FOR_UPDATED_EVENTS_ATT, lastTimeCheckedForUpdatedEvents);
            // now fetch new events
            String queryString = "select max(start) from Facet_GoogleCalendarEvent where apiKeyId=" + updateInfo.apiKey.getId() + " and calendarId='" + calendarEntry.getId() + "'";
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
                                      long since) throws IOException {
        // In the unlikely case where the server was down or disconnected more than 20 days and thus wasn't able to
        // check for updated items during this period, we need to constrain the updatedMin parameter to a maximum
        // of 20 days in the past, at the risk of getting an error from Google
        since = Math.max(since, System.currentTimeMillis()-20* DateTimeConstants.MILLIS_PER_DAY);
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
                logger.warn("updateCalendarEvents unexpected httpCode=" +
                            eventsApiCall.getLastStatusCode() + " reason=" +
                            eventsApiCall.getLastStatusMessage() +
                            " since=" + since + " message=" + e.getMessage());
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, uriTemplate, ExceptionUtils.getStackTrace(e),
                                   eventsApiCall.getLastStatusCode(), eventsApiCall.getLastStatusMessage());
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
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, uriTemplate, ExceptionUtils.getStackTrace(e),
                                   eventsApiCall.getLastStatusCode(), eventsApiCall.getLastStatusMessage());
                throw(new RuntimeException(e));
            }
        } while (pageToken != null);
    }

    private void storeEvents(final UpdateInfo updateInfo, final CalendarListEntry calendarEntry, final List<Event> eventList) throws Exception {
        for (final Event event : eventList) {
            createOrUpdateEvent(updateInfo, calendarEntry, event);
        }
    }

    private void createOrUpdateEvent(final UpdateInfo updateInfo, final CalendarListEntry calendarEntry, final Event event) throws Exception {
        if (event.getStatus().equalsIgnoreCase("cancelled")) {
            System.out.println("event " + event.getSummary() + "/" + event.getDescription() + " was canceled");
            final int deleted = jpaDaoService.execute(String.format("DELETE FROM Facet_GoogleCalendarEvent facet WHERE " +
                                                                    "facet.apiKeyId=%s AND facet.googleId='%s'",
                                                                    updateInfo.apiKey.getId(), event.getId()));
            System.out.println("deleted " + deleted + " calendar entry");
            return;
        }
        final String googleIdHash = hash(event.getId());
        final String calendarIdHash = hash(calendarEntry.getId());
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery(
                "e.apiKeyId=? AND (e.googleId=? OR e.googleId=?) AND (e.calendarId=? OR e.calendarId=?)",
                updateInfo.apiKey.getId(),
                event.getId(), googleIdHash,
                calendarEntry.getId(), calendarIdHash);
        final ApiDataService.FacetModifier<GoogleCalendarEventFacet> facetModifier = new ApiDataService.FacetModifier<GoogleCalendarEventFacet>() {
            @Override
            public GoogleCalendarEventFacet createOrModify(GoogleCalendarEventFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new GoogleCalendarEventFacet(updateInfo.apiKey.getId());
                    facet.googleId = event.getId().length()>250
                                   ? googleIdHash
                                   :event.getId();
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                    facet.calendarId = calendarEntry.getId().length()>250
                                     ? calendarIdHash
                                     : calendarEntry.getId();
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
                if (event.getRecurringEventId()!=null) {
                    final String recurringEventIdHash = hash(event.getRecurringEventId());
                    facet.recurringEventId = event.getRecurringEventId().length()>250
                                           ? recurringEventIdHash
                                           : event.getRecurringEventId();
                }
                facet.sequence = event.getSequence();
                facet.setUpdated(event.getUpdated());
                return facet;
            }
        };
        // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
        apiDataService.createOrReadModifyWrite(GoogleCalendarEventFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    @Override
    public Object syncConnectorSettings(final UpdateInfo updateInfo, Object s) {
        GoogleCalendarConnectorSettings settings = s == null
                                                 ? new GoogleCalendarConnectorSettings()
                                                 : (GoogleCalendarConnectorSettings) s;
        final List<CalendarListEntry> items = (List<CalendarListEntry>)updateInfo.getContext(REMOTE_CALLENDARS_KEY);
        final List<CalendarConfig> configs = new ArrayList<CalendarConfig>();
        for (CalendarListEntry calendarListEntry : items) {
            final String calendarId = calendarListEntry.getId();
            CalendarConfig config = settings.getCalendar(calendarId);
            if (config==null) {
                config = entry2Config(calendarListEntry);
                settings.addCalendarConfig(config);
            } else {
                config.foregroundColor = calendarListEntry.getForegroundColor();
                config.backgroundColor = calendarListEntry.getBackgroundColor();
                config.summary = calendarListEntry.getSummary();
                config.summaryOverride = calendarListEntry.getSummaryOverride();
                config.description = calendarListEntry.getDescription();
                config.primary = calendarListEntry.getPrimary()!=null?calendarListEntry.getPrimary():false;
            }
            configs.add(config);
        }
        initChannelMapping(updateInfo.apiKey, configs);
        return settings;
    }

    private Calendar getCalendar(final ApiKey apiKey) throws UpdateFailedException {
        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();
        // Get all the attributes for this connector's oauth token from the stored attributes
        final String accessToken = guestService.getApiKeyAttribute(apiKey, "accessToken");
        final String refreshToken = guestService.getApiKeyAttribute(apiKey, "refreshToken");
        final String clientId = guestService.getApiKeyAttribute(apiKey, "google.client.id");
        final String clientSecret = guestService.getApiKeyAttribute(apiKey,"google.client.secret");
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
                boolean tokenRefreshed = false;

                // Don't worry about checking if we are running on a mirrored test instance.
                // Refreshing tokens independently on both the main server and a mirrored instance
                // seems to work just fine.

                // Try to swap the expired access token for a fresh one.
                tokenRefreshed = credential.refreshToken();

                if(tokenRefreshed) {
                    Long newExpireTime = credential.getExpirationTimeMilliseconds();
                    logger.info("google calendar token has been refreshed, new expire time = " + newExpireTime);
                    // Update stored expire time
                    guestService.setApiKeyAttribute(apiKey, "accessToken", credential.getAccessToken());
                    guestService.setApiKeyAttribute(apiKey, "tokenExpires", newExpireTime.toString());
                }
            }
        }
        catch (TokenResponseException e) {
            logger.warn("module=GoogleCalendarUpdater component=background_updates action=refreshToken" +
                        " connector=" + apiKey.getConnector().getName() + " guestId=" + apiKey.getGuestId() + " status=permanently failed");
            // Notify the user that the tokens need to be manually renewed
            notificationsService.addNamedNotification(apiKey.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(),
                                                      "Heads Up. We failed in our attempt to automatically refresh your Google Calendar authentication tokens.<br>" +
                                                      "Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                      "scroll to the Google Calendar connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");

            // Record permanent update failure since this connector is never
            // going to succeed
            guestService.setApiKeyStatus(apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, Utils.stackTrace(e));
            throw new UpdateFailedException("refresh token attempt permanently failed due to a bad token refresh response", e, true);
        }
        catch (IOException e) {
            logger.warn("module=GoogleCalendarUpdater component=background_updates action=refreshToken" +
                        " connector=" + apiKey.getConnector().getName() + " guestId=" + apiKey.getGuestId() + " status=temporarily failed");
            // Notify the user that the tokens need to be manually renewed
            throw new UpdateFailedException("refresh token attempt failed", e, true);
        }
        final Calendar.Builder calendarBuilder = new Calendar.Builder(httpTransport, jsonFactory, credential);
        final Calendar calendar = calendarBuilder.build();
        return calendar;
    }

    @Override
    public void syncSharedConnectorSettings(final long apiKeyId, final SharedConnector sharedConnector) {
        JSONObject jsonSettings = new JSONObject();
        if (sharedConnector.filterJson!=null)
            jsonSettings = JSONObject.fromObject(sharedConnector.filterJson);
        // get calendars, add new configs for new calendars...
        // we use the data in the connector settings, which have either just been synched (see UpdateWorker's syncSettings)
        // or were synched  when the connector was last updated; in either cases, we know that the data is up-to-date
        final GoogleCalendarConnectorSettings connectorSettings = (GoogleCalendarConnectorSettings)settingsService.getConnectorSettings(apiKeyId);
        final List<CalendarConfig> calendars = connectorSettings.calendars;

        JSONArray sharingSettingsCalendars = new JSONArray();
        if (jsonSettings.has("calendars"))
            sharingSettingsCalendars = jsonSettings.getJSONArray("calendars");
        there: for (CalendarConfig calendarConfig : calendars) {
            for (int i=0; i<sharingSettingsCalendars.size(); i++) {
                JSONObject sharingSettingsCalendar = sharingSettingsCalendars.getJSONObject(i);
                if (sharingSettingsCalendar.getString("id").equals(calendarConfig.id))
                    continue there;
            }
            JSONObject sharingConfig = new JSONObject();
            sharingConfig.accumulate("id", calendarConfig.id);
            sharingConfig.accumulate("summary", calendarConfig.summary);
            sharingConfig.accumulate("description", calendarConfig.description);
            sharingConfig.accumulate("shared", false);
            sharingSettingsCalendars.add(sharingConfig);
        }

        // and remove configs for deleted notebooks - leave others untouched
        JSONArray settingsToDelete = new JSONArray();
        there: for (int i=0; i<sharingSettingsCalendars.size(); i++) {
            JSONObject sharingSettingsCalendar = sharingSettingsCalendars.getJSONObject(i);
            for (CalendarConfig calendarConfig : calendars) {
                if (sharingSettingsCalendar.getString("id").equals(calendarConfig.id))
                    continue there;
            }
            settingsToDelete.add(sharingSettingsCalendar);
        }
        for (int i=0; i<settingsToDelete.size(); i++) {
            JSONObject toDelete = settingsToDelete.getJSONObject(i);
            for (int j=0; j<sharingSettingsCalendars.size(); j++) {
                if (sharingSettingsCalendars.getJSONObject(j).getString("id").equals(toDelete.getString("id"))) {
                    sharingSettingsCalendars.remove(j);
                }
            }
        }
        jsonSettings.put("calendars", sharingSettingsCalendars);
        String toPersist = jsonSettings.toString();
        coachingService.setSharedConnectorFilter(sharedConnector.getId(), toPersist);
    }
}
