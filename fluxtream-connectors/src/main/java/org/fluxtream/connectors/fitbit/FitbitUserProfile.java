package org.fluxtream.connectors.fitbit;

import org.fluxtream.core.domain.AbstractUserProfile;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity(name="FitbitUserProfile")
@NamedQueries ( {
	@NamedQuery( name="fitbitUser.byEncodedId",
			query="SELECT fitbitUser FROM FitbitUserProfile fitbitUser WHERE fitbitUser.encodedId=?"),
    @NamedQuery( name="fitbitUser.byApiKeyId",
            query="SELECT fitbitUser FROM FitbitUserProfile fitbitUser WHERE fitbitUser.apiKeyId=?"),
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

    public String memberSince;
    public String glucoseUnit;
    public String heightUnit;
    public String waterUnit;
    public String weightUnit;
    public String avatar;
    public String avatar150;
    public String startDayOfWeek;

}
