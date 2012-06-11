package com.fluxtream.mvc.models;

import com.fluxtream.domain.GuestAddress;

public class AddressModel {

	public AddressModel(GuestAddress guestAddress) {
		this.address = guestAddress.address;
		this.latitude = guestAddress.latitude;
		this.longitude = guestAddress.longitude;
	}

	public String address;
	public double latitude, longitude;

}
