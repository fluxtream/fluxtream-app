package com.fluxtream.mvc.models;

import com.fluxtream.domain.Guest;

public class GuestModel {

	public String username;
	public String firstname, lastname;

	public GuestModel(Guest guest) {
		this.username = guest.username;
		this.firstname = guest.firstname;
		this.lastname = guest.lastname;
	}
	
}
