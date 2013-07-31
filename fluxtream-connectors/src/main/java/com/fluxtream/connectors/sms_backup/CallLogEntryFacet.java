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
@ObjectTypeSpec(name = "call_log", value = 1, parallel=true, prettyname = "Call Log")
//@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@NamedQueries({
		@NamedQuery(name = "sms_backup.call_log.byEmailId", query = "SELECT facet FROM Facet_CallLog facet WHERE facet.guestId=? AND facet.emailId=?"),
		@NamedQuery(name = "sms_backup.call_log.deleteAll", query = "DELETE FROM Facet_CallLog facet WHERE facet.guestId=?"),
		@NamedQuery(name = "sms_backup.call_log.between", query = "SELECT facet FROM Facet_CallLog facet WHERE facet.guestId=? AND facet.start>=? AND facet.end<=?")
})
@Indexed
public class CallLogEntryFacet extends AbstractFacet implements Serializable {

    public CallLogEntryFacet() { super(); }

	public CallLogEntryFacet(long apiKeyId) { super(apiKeyId); }
	
	public CallLogEntryFacet(Message message, long apiKeyId)
		throws IOException, MessagingException
	{
        super(apiKeyId);
        List<String> lines = IOUtils.readLines(new StringReader((String) message.getContent()));
		if (lines.size()==2) {
			String timeLine = lines.get(0);
			String callLine = lines.get(1);
			StringTokenizer st = new StringTokenizer(timeLine);
			String secsString = st.nextToken();
			seconds = Integer.parseInt(secsString.substring(0,secsString.length()-1));
			st = new StringTokenizer(callLine);
			if (callLine.indexOf("outgoing call")!=-1) {
                callType = CallType.OUTGOING;
			} else if (callLine.indexOf("incoming call")!=-1) {
                callType = CallType.INCOMING;
			}
			personNumber = st.nextToken();
			switch(callType) {
                case OUTGOING:
                    Address[] recipients = message.getRecipients(RecipientType.TO);
                    if (recipients != null && recipients.length > 0)
                        personName = ((InternetAddress)recipients[0]).getPersonal();
                    else
                        personName = message.getSubject().substring(10);//read the name from the subject line
                    break;
                case INCOMING:
                    Address[] senders = message.getFrom();
                    if (senders != null && senders.length > 0)
                        personName = ((InternetAddress)senders[0]).getPersonal();
                    else
                        personName = message.getSubject().substring(10);//read the name from the subject line
			}
		} else if (lines.size()==1) {
			String callLine = lines.get(0);
			StringTokenizer st = new StringTokenizer(callLine);
			personNumber = st.nextToken();
            callType = CallType.MISSED;
			Address[] senders = message.getFrom();
            if (senders != null && senders.length > 0)
			    personName = ((InternetAddress)senders[0]).getPersonal();
            else
                personName = message.getSubject().substring(10);//read the name from the subject line
		}
		date = message.getReceivedDate();
		this.start = date.getTime();
		this.end = date.getTime() + seconds*1000;
        this.emailId = message.getHeader("Message-ID")[0];
	}
	
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
	public void setTimeZone(TimeZone tz) {
		Calendar c = Calendar.getInstance(tz);
		startMinute = c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
		endMinute = startMinute + seconds*60;
	}
	
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
