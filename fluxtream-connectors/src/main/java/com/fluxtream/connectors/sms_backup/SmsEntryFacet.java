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
@ObjectTypeSpec(name = "sms", value = 2, parallel=true, prettyname = "Text Messages")
@NamedQueries({
		@NamedQuery(name = "sms_backup.sms.byEmailId", query = "SELECT facet FROM Facet_SmsEntry facet WHERE facet.apiKeyId=? AND facet.emailId=?")
})
@Indexed
public class SmsEntryFacet extends AbstractFacet implements Serializable {

	private static final String UNKNOWN = "Unknown";

    public SmsEntryFacet(){super();}

	public SmsEntryFacet(Message message, String username, long apiKeyId)
		throws MessagingException, IOException
	{
        super(apiKeyId);
        InternetAddress[] senders = (InternetAddress[]) message.getFrom();
        InternetAddress[] recipients = (InternetAddress[]) message.getRecipients(RecipientType.TO);
        String fromAddress, toAddress;
        boolean senderMissing = false, recipientsMissing = false;
        if (senders != null && senders.length > 0){
            fromAddress = senders[0].getAddress();
        }
        else{
            fromAddress = message.getSubject().substring(9);
            senderMissing = true;
        }
        if (recipients != null && recipients.length > 0){
            toAddress =  recipients[0].getAddress();
        }
        else{
            toAddress = message.getSubject().substring(9);
            recipientsMissing = true;
        }
		if (fromAddress.startsWith(username)) {
			smsType = SmsType.OUTGOING;
            if (recipientsMissing){
                personName = toAddress;
                personNumber = message.getHeader("X-smssync-address")[0];
            }
            else if (toAddress.indexOf("unknown.email")!=-1) {
                personName = recipients[0].getPersonal();
                personNumber = toAddress.substring(0, toAddress.indexOf("@"));
            }
            else {
                personName = recipients[0].getPersonal();
                personNumber = message.getHeader("X-smssync-address")[0];
            }
        }else {
			smsType = SmsType.INCOMING;
            if (senderMissing){
                personName = fromAddress;
                personNumber = message.getHeader("X-smssync-address")[0];
            }
            else if (fromAddress.indexOf("unknown.email")!=-1) {
				personName = senders[0].getPersonal();
				personNumber = fromAddress.substring(0, fromAddress.indexOf("@"));
			}
            else {
				personName = senders[0].getPersonal();
                personNumber = message.getHeader("X-smssync-address")[0];
			}
		}
		dateReceived = message.getReceivedDate();
		this.start = dateReceived.getTime();
		this.end = dateReceived.getTime();
		Object content = message.getContent();
		if (content instanceof String)
			this.message = (String) message.getContent();
		else if (content instanceof MimeMultipart) {//TODO: this is an MMS and needs to be handled properly
			String contentType = ((MimeMultipart) content).getContentType();
			this.message = "message of type " + contentType;
		}
        this.emailId = message.getHeader("Message-ID")[0];
	}

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
	
	public void setTimeZone(TimeZone tz) {
		Calendar c = Calendar.getInstance(tz);
		startMinute = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
	}

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
