package org.fluxtream.mvc.models;

import org.fluxtream.domain.GuestAddress;

public class AddressModel {

	public AddressModel(GuestAddress guestAddress) {
		this.address = guestAddress.address;
        this.type = guestAddress.type;
		this.latitude = guestAddress.latitude;
		this.longitude = guestAddress.longitude;
        this.radius = guestAddress.radius;
	}

	public String address, type;
	public double latitude, longitude, radius;

}
