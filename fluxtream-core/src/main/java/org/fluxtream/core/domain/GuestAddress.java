package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Column;
import com.google.gson.annotations.Expose;

@Entity(name = "Address")
@NamedQueries({
	@NamedQuery(name = "address.when",
			query = "SELECT address FROM Address address WHERE address.guestId=? AND address.since<? and address.until>? ORDER BY address.id ASC"),
	@NamedQuery(name = "addresses.byGuestId",
		query = "SELECT address FROM Address address WHERE address.guestId=? ORDER BY address.id ASC"),
	@NamedQuery(name = "addresses.delete.all",
		query = "DELETE FROM Address address WHERE address.guestId=?"),
    @NamedQuery(name = "addresses.byType",
        query = "SELECT address FROM Address address WHERE address.guestId=? AND address.type=?"),
    @NamedQuery(name = "addresses.byType.when",
        query = "SELECT address FROM Address address WHERE address.guestId=? AND address.type=? and address.since < ? and address.until > ?")
})
public class GuestAddress extends AbstractEntity {
    public static final String TYPE_HOME = "ADDRESS_HOME";
    public static final String TYPE_WORK = "ADDRESS_WORK";
    public static final String TYPE_OTHER = "ADDRESS_OTHER";

	public GuestAddress() {
	}

	public long guestId;

    @Expose
	public String address;

    @Expose
	public double latitude;

    @Expose
    public double longitude;

    @Expose
	public long since;

    @Expose
    public double radius;

    @Expose
	public long until = Long.MAX_VALUE;

    @Expose
    public String type;

    @Expose
    @Column(insertable=false, updatable=false)
    public long id;
	
	@Lob
	public String jsonStorage;

}
