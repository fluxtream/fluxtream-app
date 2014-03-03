package org.fluxtream.connectors.withings;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class UsersListResponseBody implements Serializable {

	List<UsersListResponseUser> users;
	
	public UsersListResponseBody() {}

	public List<UsersListResponseUser> getUsers() {
		return users;
	}

	public void setUsers(List<UsersListResponseUser> users) {
		this.users = users;
	}
	
}
