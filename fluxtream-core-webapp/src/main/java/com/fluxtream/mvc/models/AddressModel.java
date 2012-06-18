package com.fluxtream.mvc.models;

import com.fluxtream.domain.GuestAddress;

public class AddressModel {

	public AddressModel(GuestAddress guestAddress) {
		this.address = guestAddress.address;
        this.type = guestAddress.type;
		this.latitude = guestAddress.latitude;
		this.longitude = guestAddress.longitude;
	}

	public String address, type;
	public double latitude, longitude;

}
