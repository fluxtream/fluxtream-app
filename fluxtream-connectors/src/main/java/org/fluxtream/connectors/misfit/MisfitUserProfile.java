package org.fluxtream.connectors.misfit;

import org.fluxtream.core.domain.AbstractUserProfile;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Created by candide on 09/02/15.
 */
@Entity(name="MisfitUserProfile")
@NamedQueries( {
        @NamedQuery( name="misfitUser.byApiKeyId",
                query="SELECT misfitUser FROM MisfitUserProfile misfitUser WHERE misfitUser.apiKeyId=?"),
})
public class MisfitUserProfile extends AbstractUserProfile {

    public MisfitUserProfile() {}

    public String misfitId;
    public String userId;
    public String email;
    public String birthday;
    public String gender;

}
