package org.fluxtream.core.connectors.location;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.annotations.ObjectTypeSpec;
import org.fluxtream.core.domain.AbstractFacet;
import com.google.api.client.util.Key;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

@Entity(name="Facet_Location")
@ObjectTypeSpec(name = "location", value = 1, prettyname = "Location")
@NamedQueries({
    @NamedQuery(name = "location.delete.all",
                query = "DELETE FROM Facet_Location location WHERE location.guestId=?"),
    @NamedQuery(name = "location.lastSeen", query = "SELECT facet FROM " +
            "Facet_Location facet WHERE " +
            "facet.guestId=? AND facet.timestampMs<? " +
            "ORDER BY facet.timestampMs DESC"),
    @NamedQuery(name = "location.nextSeen", query = "SELECT facet FROM " +
            "Facet_Location facet WHERE " +
            "facet.guestId=? AND facet.timestampMs>? " +
            "ORDER BY facet.timestampMs ASC"),
    @NamedQuery(name = "location.newest", query = "SELECT facet FROM Facet_Location facet WHERE facet.guestId=? AND (facet.apiKeyId is null OR facet.apiKeyId=?) ORDER BY facet.timestampMs DESC")
})
public class LocationFacet extends AbstractFacet implements Comparable<LocationFacet>, Serializable {

	private static final long serialVersionUID = 2882496521084143121L;

    public LocationFacet() {}

    public LocationFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    public static enum Source {
        OTHER, USER, GOOGLE_LATITUDE, GEO_IP_DB, IP_TO_LOCATION, OPEN_PATH,
        RUNKEEPER, FLUXTREAM_CAPTURE, FLICKR, MOVES, MYMEE, NONE, EVERNOTE,
        JAWBONE_UP
    }

    @Index(name="source")
	public Source source = Source.GOOGLE_LATITUDE;
	
	@Key
	@Index(name="timestamp_index")
	public long timestampMs;

	@Key
	@Index(name="latitude_index")
	public float latitude;

	@Key
	@Index(name="longitude_index")
	public float longitude;

	@Key
	public int accuracy;

	@Key
	public int speed;

	@Key
	public int heading;

	@Key
	public int altitude;

	@Key
	public int altitudeAccuracy;

	@Key
	public int placeid;

	public String device;

	public String version;

	public String os;

    @Type(type="yes_no")
    @Index(name="processed_index")
    public Boolean processed = false;

    /**
     * serves as a backreference to the resource that originated in this coordinate,
     * e.g. a runkeeper run or bike ride
     */
    @Index(name = "uri")
    public String uri;

	public boolean equals(Object o) {
		if (!(o instanceof LocationFacet))
			return false;
		LocationFacet lr = (LocationFacet) o;
		return lr.timestampMs == timestampMs && lr.latitude == this.latitude
				&& lr.longitude == this.longitude;
	}

	@Override
	public int compareTo(LocationFacet o1) {
		return (o1.timestampMs > timestampMs)?-1:1;
	}

    @Override
	protected void makeFullTextIndexable() { }
}
