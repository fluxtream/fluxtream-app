package com.fluxtream.connectors.google_calendar;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
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
@ObjectTypeSpec(name = "entry", value = 1, prettyname = "Event")
public class GoogleCalendarEventFacet extends AbstractFacet {

    @Index(name="googleId")
    public String googleId;
    @Lob
    public String etag;
    public Boolean endTimeUnspecified;
    public Boolean guestsCanSeeOtherGuests;
    @Lob
    public String hangoutLink;
    @Lob
    public String htmlLink;
    @Lob
    public String iCalUID;
    @Lob
    public String kind;
    @Lob
    public String location;
    public Boolean locked;
    @Lob
    public String status;
    @Lob
    public String colorId;
    @Lob
    public String description;
    @Lob
    public String summary;
    public long originalStartTime;
    public long created;
    @Lob
    public String attendees;
    @Lob
    public String creator;
    @Lob
    public String organizer;
    public boolean isAllDay;
    public int startTimezoneShift;
    public int endTimezoneShift;

    public GoogleCalendarEventFacet() {super();}

    public GoogleCalendarEventFacet(final Long apiKeyId) {
        super(apiKeyId);
    }


    @Override
    protected void makeFullTextIndexable() {
        fullTextDescription = summary;
    }

    public void setStart(final EventDateTime start) {
        if (start!=null) {
            if (start.getDateTime()!=null) {
                this.start = start.getDateTime().getValue();
                this.startTimezoneShift = start.getDateTime().getTimeZoneShift();
            } else if (start.getDate()!=null) {
                isAllDay = true;
                this.start = start.getDate().getValue();
                this.startTimezoneShift = start.getDate().getTimeZoneShift();
            }
        }
    }

    public void setEnd(final EventDateTime end) {
        if (end!=null) {
            if (end.getDateTime()!=null) {
                this.end = end.getDateTime().getValue();
                this.endTimezoneShift = end.getDateTime().getTimeZoneShift();
            } else if (end.getDate()!=null) {
                this.end = end.getDate().getValue();
                this.endTimezoneShift = end.getDate().getTimeZoneShift();
            }
        }
    }

    public void setOriginalStartTime(final EventDateTime originalStartTime) {
        if (originalStartTime!=null) {
            this.originalStartTime = originalStartTime.getDateTime().getValue();
            if (this.start==0) this.start = this.originalStartTime;
        }
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
