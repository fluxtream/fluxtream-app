package com.fluxtream.connectors.google_calendar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.vos.AbstractTimedFacetVO;
import com.fluxtream.domain.GuestSettings;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;

/**
 * User: candide
 * Date: 28/07/13
 * Time: 21:39
 */
public class GoogleCalendarEventFacetVO extends AbstractTimedFacetVO<GoogleCalendarEventFacet> {

    public String summary;
    public String description;
    public long apiKeyId;
    public String calendarId;
    public List<EventAttendee> attendees;
    public Event.Creator creator;
    public Event.Organizer organizer;
    public String location;
    public boolean recurringEvent;

    @Override
    protected void fromFacet(final GoogleCalendarEventFacet facet, final TimeInterval timeInterval, final GuestSettings settings) throws OutsideTimeBoundariesException {
        try {
            startMinute = toMinuteOfDay(new Date(facet.start), timeInterval.getTimeZone(facet.start));
            endMinute = toMinuteOfDay(new Date(facet.end), timeInterval.getTimeZone(facet.end));
            JacksonFactory jacksonFactory = new JacksonFactory();
            if (facet.attendeesStorage!=null) {
                this.attendees = new ArrayList<EventAttendee>();
                String[] attendees = facet.attendeesStorage.split("\\|");
                for (String attendee : attendees) {
                    final EventAttendee eventAttendee = jacksonFactory.fromString(attendee, EventAttendee.class);
                    this.attendees.add(eventAttendee);
                }
            }
            if (facet.creatorStorage!=null)
                creator = jacksonFactory.fromString(facet.creatorStorage, Event.Creator.class);
            if (facet.organizerStorage!=null)
                organizer = jacksonFactory.fromString(facet.organizerStorage, Event.Organizer.class);
            this.description = facet.description;
            this.summary = facet.summary;
            this.apiKeyId = facet.apiKeyId;
            this.calendarId = facet.calendarId;
            this.location = facet.location;
            this.recurringEvent = facet.recurringEventId!=null;
        }
        catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
