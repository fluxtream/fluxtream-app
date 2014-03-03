package org.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;

@Entity(name="ApiNotifications")
@NamedQueries ( {
	@NamedQuery( name="apiNotifications.last",
		query="SELECT notification FROM ApiNotifications notification WHERE notification.guestId=? and notification.api=? ORDER BY notification.ts DESC")
})
public class ApiNotification extends AbstractEntity {
	
	@Index(name="guestId")
	public long guestId;

	@Index(name="ts")
	public long ts;

	@Index(name="api")
	public int api;
	
	@Lob
	public String content;

}
