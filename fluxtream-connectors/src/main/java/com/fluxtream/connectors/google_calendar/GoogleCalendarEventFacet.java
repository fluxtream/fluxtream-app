package com.fluxtream.connectors.google_calendar;

import java.util.List;
import javax.persistence.Entity;
import com.fluxtream.domain.AbstractFacet;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 27/07/13
 * Time: 16:24
 */
@Entity(name="Facet_GoogleCalendarEvent")
public class GoogleCalendarEventFacet extends AbstractFacet {

    public String etag;
    public Boolean endTimeUnspecified;
    public Boolean guestsCanSeeOtherGuests;
    public String hangoutLink;
    public String htmlLink;
    public String iCalUID;
    public String kind;
    public String location;
    public Boolean locked;
    public String status;
    public String colorId;
    public String description;
    public long originalStartTime;
    public long created;
    public String attendees;
    public String creator;
    public String organizer;

    public GoogleCalendarEventFacet(final Long apiKeyId) {
        super(apiKeyId);
    }

    public String summary;

    @Index(name="googleId")
    public String googleId;

    @Override
    protected void makeFullTextIndexable() {
        fullTextDescription = summary;
    }

    public void setStart(final EventDateTime start) {
        this.start = start.getDateTime().getValue();
    }

    public void setEnd(final EventDateTime end) {
        if (end!=null)
            this.end = end.getDateTime().getValue();
    }

    public void setOriginalStartTime(final EventDateTime originalStartTime) {
        if (originalStartTime!=null)
            this.originalStartTime = originalStartTime.getDateTime().getValue();
    }

    public void setCreated(final DateTime created) {
        if (created!=null)
            this.created = created.getValue();
    }

    public void setAttendees(final List<EventAttendee> attendees) {
        if (attendees==null)
            return;
        StringBuilder sb = new StringBuilder();
        for (EventAttendee attendee : attendees) {
            if (sb.length()>0) sb.append(", ");
            sb.append(attendee.getDisplayName());
        }
        this.attendees = sb.toString();
    }

    public void setCreator(final Event.Creator creator) {
        if (creator!=null)
            this.creator = creator.getDisplayName();
    }

    public void setOrganizer(final Event.Organizer organizer) {
        if (organizer!=null)
            this.organizer = organizer.getDisplayName();
    }
}
