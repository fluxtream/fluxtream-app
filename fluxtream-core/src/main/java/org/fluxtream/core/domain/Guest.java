package org.fluxtream.core.domain;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(name="Guest")
@NamedQueries ( {
	@NamedQuery(name = "guests.count",
			query = "SELECT COUNT(guest) FROM Guest"),
	@NamedQuery( name="guest.byEmail",
			query="SELECT guest FROM Guest guest WHERE guest.email=?"),
	@NamedQuery( name="guest.byUsername",
			query="SELECT guest FROM Guest guest WHERE guest.username=?"),
    @NamedQuery( name="guest.byAutoLoginToken",
                 query="SELECT guest FROM Guest guest WHERE guest.autoLoginToken=?"),
	@NamedQuery( name="guests.all",
			query="SELECT guest FROM Guest guest")
})
public class Guest extends AbstractEntity implements Serializable {

	public static final String ROLE_USER = "ROLE_USER";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public String appId;

    public enum RegistrationMethod {
        REGISTRATION_METHOD_FORM, REGISTRATION_METHOD_FACEBOOK, REGISTRATION_METHOD_FACEBOOK_WITH_PASSWORD,
        REGISTRATION_METHOD_API;
    }

	@Index(name="username_index")
	public String username;
	public String firstname, lastname, password;
	@Index(name="email_index")
	public String email;
	public String salt;
    public String autoLoginToken;
    public Long autoLoginTokenTimestamp;
    public RegistrationMethod registrationMethod;

	transient List<String> userRoles;
	String roles = ROLE_USER;

	public Guest() {}

	public boolean hasRole(String role) {
		return getUserRoles().contains(role);
	}
	
	public List<String> getUserRoles() {
		if (userRoles==null) {
			userRoles = new ArrayList<String>();
            final String[] splits = StringUtils.split(roles, ",");
			for(int i=0; i<splits.length; i++)
				userRoles.add(""+splits[i].trim());
		}
		return userRoles;
	}

    public boolean equals(Object o) {
        if (! (o instanceof Guest))
            return false;
        return ((Guest)o).getId() == getId();
    }

	public String getGuestName() {
		if (!StringUtils.isEmpty(firstname)){
			if (!StringUtils.isEmpty(lastname))
				return firstname + " " + lastname;
			else return firstname;
		}
		else return username;
	}

    public void setRoles(final String roles) {
        this.roles = roles;
    }
}
