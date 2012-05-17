package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name = "Address")
@NamedQueries({
	@NamedQuery(name = "address.when",
			query = "SELECT address FROM Address address WHERE address.guestId=? AND address.since<? and address.until>?"),
	@NamedQuery(name = "addresses.byGuestId",
		query = "SELECT address FROM Address address WHERE address.guestId=? ORDER BY address.since DESC"),
	@NamedQuery(name = "addresses.delete.all",
		query = "DELETE FROM Address address WHERE address.guestId=?"),
    @NamedQuery(name = "addresses.byType",
        query = "SELECT address FROM Address address WHERE address.guestId=? AND address.type=?"),
    @NamedQuery(name = "addresses.byType.when",
        query = "SELECT address FROM Address address WHERE address.guestId=? AND address.type=? and address.since < ? and address.until > ?")
})
public class GuestAddress extends AbstractEntity {

	public GuestAddress() {
	}

	public long guestId;
	
	public String address;
	
	public double latitude, longitude;
	
	public long since;
	
	// not used, but let's keep it for nows
	public long until = Long.MAX_VALUE;

    public String type;
	
	@Lob
	public String jsonStorage;

}
