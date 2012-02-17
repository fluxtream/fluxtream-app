package com.fluxtream.mvc.models;

import com.fluxtream.domain.GuestAddress;

public class HomeAddressModel {

	boolean isSet = false;

	public HomeAddressModel() {}

	public HomeAddressModel(GuestAddress guestAddress) {
		this.address = guestAddress.address;
		this.latitude = guestAddress.latitude;
		this.longitude = guestAddress.longitude;
		isSet = true;
	}

	public String address;
	public double latitude = -1, longitude = -1;

}
