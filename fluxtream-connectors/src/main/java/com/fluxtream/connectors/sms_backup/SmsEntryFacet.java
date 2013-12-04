package com.fluxtream.connectors.sms_backup;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.mail.Address;
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
@ObjectTypeSpec(name = "sms", value = 2, parallel=true, prettyname = "Text Messages", extractor=SmsEntryFacetExtractor.class)
@NamedQueries({
		@NamedQuery(name = "sms_backup.sms.byEmailId", query = "SELECT facet FROM Facet_SmsEntry facet WHERE facet.apiKeyId=? AND facet.emailId=?")
})
@Indexed
public class SmsEntryFacet extends AbstractFacet implements Serializable {

	private static final String UNKNOWN = "Unknown";

    public SmsEntryFacet(){super();}
    public SmsEntryFacet(long apiKeyId){super(apiKeyId);}
	public static enum SmsType {
		INCOMING, OUTGOING
	}

	SmsType smsType;

	public String personName;
	public String personNumber;
    public String emailId;
	@Lob
	public String message;
	public Date dateReceived;
	transient public int startMinute;

	public String toString() {
		String s = "Sms ";
		switch(smsType) {
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
