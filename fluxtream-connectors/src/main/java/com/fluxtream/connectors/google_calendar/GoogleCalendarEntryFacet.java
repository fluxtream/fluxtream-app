package com.fluxtream.connectors.google_calendar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.hibernate.search.annotations.Indexed;

import com.fluxtream.domain.AbstractFacet;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.TextContent;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.EventWho;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;

@SuppressWarnings("serial")
@Entity(name="Facet_CalendarEventEntry")
@ObjectTypeSpec(name = "entry", value = 1, parallel=true, prettyname = "Entry")
@NamedQueries({
        @NamedQuery(name = "google_calendar.entry.newest", query = "SELECT facet FROM Facet_CalendarEventEntry facet WHERE facet.guestId=? ORDER BY facet.end DESC LIMIT 1")
})
@Indexed
public class GoogleCalendarEntryFacet extends AbstractFacet implements Serializable {

	public String icalUID;
	public String entryId;
	public String kind;
	
	@Lob
	@Column(length=100000)
	public String plainTextContent;
	
	@Lob
	public String linkHref;
	@Lob
	public String linkTitle;
	@Lob
	public String summary;
	
	@Lob
	@Column(length=100000)
	public String textContent;
	public long published;
	public long edited;
	@Lob
	public String title;
	
	public transient List<SWhere> locations;
	public transient List<SEventWho> participants;
	public transient List<SWhen> times;
	
	@Lob
	byte[] whenStorage;
	@Lob
	byte[] participantsStorage;
	@Lob
	byte[] locationsStorage;
	
	public GoogleCalendarEntryFacet() {}
	
	@SuppressWarnings("unchecked")
	@PostLoad
	void deserialize() {
		try {
			if (whenStorage != null) {
				ObjectInputStream objectinput = new ObjectInputStream(
						new ByteArrayInputStream(whenStorage));
				times = (List<SWhen>) objectinput.readObject();
			}
			if (participantsStorage != null) {
				ObjectInputStream objectinput = new ObjectInputStream(
						new ByteArrayInputStream(participantsStorage));
				participants = (List<SEventWho>) objectinput.readObject();
			}
			if (locationsStorage != null) {
				ObjectInputStream objectinput = new ObjectInputStream(
						new ByteArrayInputStream(locationsStorage));
				locations = (List<SWhere>) objectinput.readObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@PrePersist
	void serialize() {
		try {
			if (times!=null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream objectoutput = new ObjectOutputStream(baos);
				objectoutput.writeObject(times);
				whenStorage = baos.toByteArray();
			}
			if (participants!=null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream objectoutput = new ObjectOutputStream(baos);
				objectoutput.writeObject(participants);
				participantsStorage = baos.toByteArray();
			}
			if (locations!=null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream objectoutput = new ObjectOutputStream(baos);
				objectoutput.writeObject(locations);
				locationsStorage = baos.toByteArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public GoogleCalendarEntryFacet(CalendarEventEntry entry) {
//		entry.getAuthors();
//		entry.getCategories();
//		entry.getContent();
//		entry.getContributors();
		DateTime edited = entry.getEdited();
		if (edited!=null) this.edited = edited.getValue();
//		entry.getEditLink();
//		entry.getEtag();
		Link htmlLink = entry.getHtmlLink();
		linkHref = htmlLink.getHref();
		linkTitle = htmlLink.getTitle();
		icalUID = entry.getIcalUID();
		entryId = entry.getId();
		kind = entry.getKind();
//		entry.getLinks();
		List<Where> locations = entry.getLocations();
		if (locations!=null) {
			this.locations = new ArrayList<SWhere>();
			for (Where where : locations) {
				SWhere swhere = new SWhere();
				swhere.label = where.getLabel();
				swhere.valueString = where.getValueString();
				this.locations.add(swhere);
			}
		}
//		entry.getMediaEditLink();
//		entry.getOriginalEvent();
		List<EventWho> participants = entry.getParticipants();
		if (participants!=null) {
			this.participants = new ArrayList<SEventWho>();
			for (EventWho eventWho : participants) {
				SEventWho seventWho = new SEventWho();
				seventWho.email = eventWho.getEmail();
				seventWho.attendeeStatus = eventWho.getAttendeeStatus();
				seventWho.attendeeType = eventWho.getAttendeeType();
				seventWho.valueString = eventWho.getValueString();
				this.participants.add(seventWho);
			}
		}
		plainTextContent = entry.getPlainTextContent();
		DateTime published = entry.getPublished();
		if (published!=null) this.published = published.getValue();
//		entry.getRecurrence();
//		entry.getReminder();
		List<When> times = entry.getTimes();
		if (times!=null) {
			this.times = new ArrayList<SWhen>();
			for (When when : times) {
				SWhen swhen = new SWhen();
				swhen.startTime = when.getStartTime().getValue();
				swhen.endTime = when.getEndTime().getValue();
				this.times.add(swhen);
			}
		}
		TextConstruct summary = entry.getSummary();
		if (summary!=null) this.summary = summary.getPlainText();
		TextContent textContent = entry.getTextContent();
		if (textContent!=null&&textContent.getContent()!=null)
			this.textContent = textContent.getContent().getPlainText();
		TextConstruct title = entry.getTitle();
		if (title!=null) this.title = title.getPlainText();
	}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = "";
		if (plainTextContent!=null)
			fullTextDescription += plainTextContent;
		if (textContent!=null)
			fullTextDescription += " " + textContent;
		if (title!=null)
			fullTextDescription += " " + title;
	}
	
	
}
