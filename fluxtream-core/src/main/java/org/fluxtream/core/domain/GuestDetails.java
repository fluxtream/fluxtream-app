package org.fluxtream.core.domain;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * User: candide
 * Date: 18/11/13
 * Time: 07:55
 */
@Entity(name = "GuestDetails")
@NamedQueries({
    @NamedQuery(name = "guestDetails.byUsernmae",
        query = "SELECT details from GuestDetails details WHERE details.guestId=?"),
    @NamedQuery(name = "guestDetails.byGuestId",
        query = "SELECT details from GuestDetails details WHERE details.guestId=?"),
    @NamedQuery(name = "guestDetails.delete.all",
        query = "DELETE from GuestDetails details WHERE details.guestId=?")
})
public class GuestDetails extends AbstractEntity {

    @Index(name="guestId")
    public long guestId;
    public String phoneNumber;

    public String avatarImageURL;

    public enum CoachCategory {
        NONE, FREE, TEST_DRIVE, PRO
    }

    @Lob
    public String profilePresentation;

    public String countryCode;
    public Integer city_id;

    public String website;

    public String accessToken;
    public String refreshToken;
    public long expires;

    public GuestDetails(){}
    public GuestDetails(long guestId) {
        this.guestId = guestId;
    }

    public CoachCategory coachCategory = CoachCategory.NONE;

    private static final String TAG_DELIMITER = ",";

    @Lob
    public String parseInstallationsStorage;

    public transient Set<String> parseInstallations;

    public Set<String> getInstallations() {
        if (parseInstallations==null) parseInstallations = new HashSet<String>();
        return Collections.unmodifiableSet(parseInstallations);
    }

    @PostLoad
    void loadInstallations() {
        if (parseInstallationsStorage == null || parseInstallationsStorage.equals("")) {
            return;
        }
        StringTokenizer st = new StringTokenizer(parseInstallationsStorage,", \t\n\r\f");
        while (st.hasMoreTokens()) {
            String tag = st.nextToken().trim();
            if (tag.length() > 0) {
                addInstallation(tag);
            }
        }
    }

    public void addInstallation(final String parseInstallationId) {
        if (parseInstallationId != null && parseInstallationId.length() > 0) {
            if (parseInstallations == null) {
                parseInstallations = new HashSet<String>();
            }
            parseInstallations.add(parseInstallationId);
        }
    }

    @PrePersist
    @PreUpdate
    protected void persistInstallations() {
        if (parseInstallations == null) {
            return;
        }
        parseInstallationsStorage = StringUtils.join(parseInstallations,",");
    }
}