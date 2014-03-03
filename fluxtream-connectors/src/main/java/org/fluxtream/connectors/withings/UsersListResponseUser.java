package org.fluxtream.connectors.withings;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UsersListResponseUser implements Serializable {

	long id;
	String firstname;
	String lastname;
	String shortname;
	int gender;
	String fatmethod;
	long birthdate;
	int ispublic;
	String publickey;
	
	public UsersListResponseUser(){}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getFirstname() {
		return firstname;
	}
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
	public String getLastname() {
		return lastname;
	}
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
	public String getShortname() {
		return shortname;
	}
	public void setShortname(String shortname) {
		this.shortname = shortname;
	}
	public int getGender() {
		return gender;
	}
	public void setGender(int gender) {
		this.gender = gender;
	}
	public String getFatmethod() {
		return fatmethod;
	}
	public void setFatmethod(String fatmethod) {
		this.fatmethod = fatmethod;
	}
	public long getBirthdate() {
		return birthdate;
	}
	public void setBirthdate(long birthdate) {
		this.birthdate = birthdate;
	}
	public int getIspublic() {
		return ispublic;
	}
	public void setIspublic(int ispublic) {
		this.ispublic = ispublic;
	}
	public String getPublickey() {
		return publickey;
	}
	public void setPublickey(String publickey) {
		this.publickey = publickey;
	}
	
}
