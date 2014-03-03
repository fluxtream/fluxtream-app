package org.fluxtream.connectors.sms_backup;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.domain.AbstractFacet;
import org.hibernate.search.annotations.Indexed;

@SuppressWarnings("serial")
@Entity(name="Facet_SmsEntry")
@ObjectTypeSpec(name = "sms", value = 2, parallel=true, isImageType=true, prettyname = "Text Messages", extractor=SmsEntryFacetExtractor.class, photoFacetFinderStrategy=SmsBackupPhotoFacetFinderStrategy.class)
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

    public Boolean hasAttachments;
    @Lob
    public String attachmentNames;
    public String attachmentMimeTypes;



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
