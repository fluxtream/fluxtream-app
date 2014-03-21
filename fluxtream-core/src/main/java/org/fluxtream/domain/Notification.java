package org.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;

@Entity(name = "Notifications")
@NamedQueries({
    @NamedQuery(name = "notifications.withName", query = "SELECT notification "
                                                  + "FROM Notifications notification "
                                                  + "WHERE notification.guestId=? AND notification.name=?"),
	@NamedQuery(name = "notifications.all", query = "SELECT notification "
			+ "FROM Notifications notification "
			+ "WHERE notification.guestId=? AND notification.deleted=false "
			+ "ORDER BY notification.ts DESC"),
    @NamedQuery(name="notifications.withTypeAndMessage", query="SELECT notification "
            + "FROM Notifications notification WHERE "
            + "notification.guestId=? AND notification.deleted=false "
            + "AND notification.type=? AND notification.message=?"),
	@NamedQuery(name = "notifications.delete.all",
		query = "DELETE FROM Notifications notification WHERE notification.guestId=?") }
)
public class Notification extends AbstractEntity {

    public static enum Type {
		WARNING, ERROR, INFO
	}

	public Type type;

    public String name;

	@Index(name = "guestId_index")
	public long guestId;

	@org.hibernate.annotations.Type(type = "yes_no")
	public boolean deleted;

	@Lob
	public String message;

    public int repeated = 1;

    @Lob
    public String stackTrace;

	public long ts;

	public Notification() {
		ts = System.currentTimeMillis();
	}

}
