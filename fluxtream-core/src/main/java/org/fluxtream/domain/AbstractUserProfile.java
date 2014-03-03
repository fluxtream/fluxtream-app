package org.fluxtream.domain;

import java.util.Date;
import java.util.TimeZone;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractUserProfile extends AbstractEntity {

	public long guestId;
	public long firstSeenHere;
	
	public Date firstSeenHere() {
		return new Date(firstSeenHere);
	}
	
	public abstract TimeZone getTimeZone();
	
}
