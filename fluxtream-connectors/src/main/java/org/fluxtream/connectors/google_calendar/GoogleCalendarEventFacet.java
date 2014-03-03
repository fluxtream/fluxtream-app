package org.fluxtream.connectors.google_calendar;

import java.sql.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Lob;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractRepeatableFacet;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.gson.Gson;
import org.hibernate.annotations.Index;
import org.joda.time.DateTimeConstants;

/**
 * User: candide
 * Date: 27/07/13
 * Time: 16:24
 */
@Entity(name="Facet_GoogleCalendarEvent")
@ObjectTypeSpec(name = "entry", value = 1, prettyname = "Event", isMixedType = true)
public class GoogleCalendarEventFacet extends AbstractRepeatableFacet {

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
    public long eventUpdated;
    @Lob
    public String creatorStorage;
    @Lob
    public String organizerStorage;
    public int startTimezoneShift;
    public int endTimezoneShift;
    public String calendarId;

    public String transparency;
    public String visibility;
    public Integer sequence;

    @Lob
    public String attendeesStorage;

    @Lob
    public String recurrence;

    static Gson gson;

    public GoogleCalendarEventFacet() {super();}

    public GoogleCalendarEventFacet(final Long apiKeyId) {
        super(apiKeyId);
    }

    public String recurringEventId;

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
                this.allDayEvent = true;
                this.startDate = new Date(start.getDate().getValue());
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
                this.allDayEvent = true;
                this.endDate = new Date(end.getDate().getValue()- DateTimeConstants.MILLIS_PER_DAY);
                this.end = end.getDate().getValue();
                this.endTimezoneShift = end.getDate().getTimeZoneShift();
            }
        }
    }

    public void setOriginalStartTime(final EventDateTime originalStartTime) {
        if (originalStartTime!=null) {
            if (originalStartTime.getDateTime()!=null) {
                this.originalStartTime = originalStartTime.getDateTime().getValue();
                this.start = originalStartTime.getDateTime().getValue();
            } else if (originalStartTime.getDate()!=null) {
                this.allDayEvent = true;
                this.originalStartTime = originalStartTime.getDate().getValue();
                this.start = originalStartTime.getDate().getValue();
            }
        }
    }

    public void setCreated(final DateTime created) {
        if (created!=null)
            this.created = created.getValue();
    }

    public void setAttendees(final List<EventAttendee> attendees) {
        if (attendees==null) return;
        StringBuilder sb = new StringBuilder();
        for (EventAttendee attendee : attendees) {
            if (sb.length()>0) sb.append("|");
            sb.append(attendee.toString());
        }
        attendeesStorage = sb.toString();
    }

    public void setCreator(final Event.Creator creator) {
        if (creator!=null)
            this.creatorStorage = creator.toString();
    }

    public void setOrganizer(final Event.Organizer organizer) {
        if (organizer!=null)
            organizerStorage = organizer.toString();
    }

    public void setRecurrence(final List<String> recurrence) {
        if (recurrence!=null&&recurrence.size()>0) {
            StringBuilder sb = new StringBuilder();
            for (String s : recurrence) {
                if (sb.length()>0) sb.append("|");
                sb.append(s);
            }
            this.recurrence = sb.toString();
        }
    }

    public void setUpdated(final DateTime updated) {
        if (updated!=null)
            this.eventUpdated = updated.getValue();
    }
}
