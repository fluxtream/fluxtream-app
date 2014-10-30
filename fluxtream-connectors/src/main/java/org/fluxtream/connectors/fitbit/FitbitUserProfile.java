package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.domain.AbstractUserProfile;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.util.TimeZone;

@Entity(name="FitbitUserProfile")
@NamedQueries ( {
	@NamedQuery( name="fitbitUser.byEncodedId",
			query="SELECT fitbitUser FROM FitbitUserProfile fitbitUser WHERE fitbitUser.encodedId=?"),
    @NamedQuery( name="fitbitUser.byGuestId",
            query="SELECT fitbitUser FROM FitbitUserProfile fitbitUser WHERE fitbitUser.guestId=?"),
    @NamedQuery( name="fitbitUser.delete",
            query="DELETE FROM FitbitUserProfile fitbitUser WHERE fitbitUser.guestId=?")
})
public class FitbitUserProfile extends AbstractUserProfile {

	public String aboutMe;
	public String city;
	public String country;
	public String dateOfBirth;
	public String displayName;
	public String encodedId;
	public String fullName;
	public String gender;
	public double height;
	public String nickname;
	public long offsetFromUTCMillis;
	public String state;
	public double strideLengthRunning;
	public double strideLengthWalking;
	public String timezone;
	public double weight;
	
	@Override
	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone(timezone);
	}
	
}
