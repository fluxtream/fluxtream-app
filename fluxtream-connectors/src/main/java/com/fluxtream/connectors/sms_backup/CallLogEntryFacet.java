package com.fluxtream.connectors.sms_backup;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.commons.io.IOUtils;
import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import com.ibm.icu.util.StringTokenizer;

@SuppressWarnings("serial")
@Entity(name="Facet_CallLog")
@ObjectTypeSpec(name = "call_log", value = 1, parallel=true, prettyname = "Call Log", extractor=CallLogFacetExtractor.class)
//@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
		@NamedQuery(name = "sms_backup.call_log.byEmailId", query = "SELECT facet FROM Facet_CallLog facet WHERE facet.apiKeyId=? AND facet.emailId=?")
})
@Indexed
public class CallLogEntryFacet extends AbstractFacet implements Serializable {

    public CallLogEntryFacet() { super(); }

	public CallLogEntryFacet(long apiKeyId) { super(apiKeyId); }
	
	public static enum CallType {
		INCOMING, OUTGOING, MISSED
	}
	
	public CallType callType;

	public String personName;
	public String personNumber;
	public int seconds;
	public Date date;
    public String emailId;
	transient public int startMinute;
	transient public int endMinute;
	
	public String toString() {
		String s = date.toString();
		switch(callType) {
		case OUTGOING:
			s += ": called " + personName + " (" + personNumber + ")";
			break;
		case INCOMING:
			s += ": call from " + personName + " (" + personNumber + ")";
			break;
		default:
			s += ": missed call from " + personName + " (" + personNumber + ")";
			break;
		}
		return s;
	}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = toString();
	}
	
}
