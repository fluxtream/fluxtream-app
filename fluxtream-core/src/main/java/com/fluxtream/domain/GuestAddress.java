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
		query = "DELETE FROM Address address WHERE address.guestId=?")
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
	
	@Lob
	public String jsonStorage;

}
