package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="ResetPasswordToken")
@NamedQueries ( {
	@NamedQuery( name="passwordToken.byToken",
			query="SELECT token from ResetPasswordToken token WHERE token.token=?")
})
public class ResetPasswordToken extends AbstractEntity {

	public ResetPasswordToken() {}
	
	public String token;
	public long guestId;
	
	public long ts;
	
	
}
