package com.fluxtream.connectors.sms_backup;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.search.annotations.Indexed;

import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;

@SuppressWarnings("serial")
@Entity(name="Facet_SmsEntry")
@ObjectTypeSpec(name = "sms", value = 2, parallel=true, prettyname = "Text Messages")
@NamedQueries({
		@NamedQuery(name = "sms_backup.sms.byStartEnd", query = "SELECT facet FROM Facet_SmsEntry facet WHERE facet.guestId=? AND facet.start=? AND facet.end=?"),
		@NamedQuery(name = "sms_backup.sms.deleteAll", query = "DELETE FROM Facet_SmsEntry facet WHERE facet.guestId=?"),
		@NamedQuery(name = "sms_backup.sms.between", query = "SELECT facet FROM Facet_SmsEntry facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class SmsEntryFacet extends AbstractFacet implements Serializable {

	private static final String UNKNOWN = "Unknown";

	public SmsEntryFacet() {}
	
	public SmsEntryFacet(Message message, String username)
		throws MessagingException, IOException
	{
		InternetAddress from = (InternetAddress) message.getFrom()[0];
		String fromAddress = from.getAddress();
		if (fromAddress.startsWith(username)) {
			type = SmsType.OUTGOING;
			InternetAddress to = (InternetAddress) message.getRecipients(RecipientType.TO)[0];
			String toAddress = to.getAddress();
			if (toAddress.indexOf("unknown.email")!=-1) {
				personName = UNKNOWN;
				personNumber = toAddress.substring(0, toAddress.indexOf("@"));
			} else {
				personName = to.getPersonal();
			}
		} else {
			type = SmsType.INCOMING;
			if (fromAddress.indexOf("unknown.email")!=-1) {
				personName = UNKNOWN;
				personNumber = fromAddress.substring(0, fromAddress.indexOf("@"));
			} else {
				personName = from.getPersonal();
			}
		}
		System.out.print("-");
		dateReceived = message.getReceivedDate();
		this.start = dateReceived.getTime();
		this.end = dateReceived.getTime();
		Object content = message.getContent();
		if (content instanceof String)
			this.message = (String) message.getContent();
		else if (content instanceof MimeMultipart) {
			String contentType = ((MimeMultipart) content).getContentType();
			this.message = "message of type " + contentType;
		}
	}

	public static enum SmsType {
		INCOMING, OUTGOING
	}

	SmsType type;

	public String personName;
	public String personNumber;
	@Lob
	public String message;
	public Date dateReceived;
	transient public int startMinute;
	
	public void setTimeZone(TimeZone tz) {
		Calendar c = Calendar.getInstance(tz);
		startMinute = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
	}

	public String toString() {
		String s = "Sms ";
		switch(type) {
		case INCOMING:
			s += "from " + (personName.equals(UNKNOWN)?personNumber:personName) + ": " + message;
			break;
		case OUTGOING:
			s += "to " + (personName.equals(UNKNOWN)?personNumber:personName) + ": " + message;
			break;
		}
		return s;
	}

	@Override
	protected void makeFullTextIndexable() {
		this.fullTextDescription = personName + " " + personNumber + " " + message;
	}
}
