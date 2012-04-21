package com.fluxtream.connectors.google_latitude;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Index;

import com.fluxtream.domain.AbstractFacet;
import com.google.api.client.googleapis.json.JsonCContent;
import com.google.api.client.util.Key;

@Entity(name="Facet_GoogleLatitudeLocation")
@NamedQueries({
		@NamedQuery(name = "google_latitude.lastSeen", query = "SELECT facet FROM " +
				"Facet_GoogleLatitudeLocation facet WHERE " +
				"facet.guestId=? AND facet.timestampMs<? " +
				"ORDER BY facet.timestampMs DESC"),
		@NamedQuery(name = "google_latitude.nextSeen", query = "SELECT facet FROM " +
				"Facet_GoogleLatitudeLocation facet WHERE " +
				"facet.guestId=? AND facet.timestampMs>? " +
				"ORDER BY facet.timestampMs ASC"),
		@NamedQuery(name = "google_latitude.deleteAll", query = "DELETE FROM " +
				"Facet_GoogleLatitudeLocation facet WHERE " +
				"facet.guestId=?"),
		@NamedQuery(name = "google_latitude.between", query = "SELECT facet FROM " +
				"Facet_GoogleLatitudeLocation facet WHERE " +
				"facet.guestId=? AND facet.timestampMs>=? AND " +
				"facet.timestampMs<=? AND " +
				"(facet.source=0 or facet.source=null)")
})
public class LocationFacet extends AbstractFacet implements Comparable<LocationFacet>, Serializable {

	private static final long serialVersionUID = 2882496521084143121L;

	public static enum Source {
		GOOGLE_LATITUDE, GEO_IP, OPEN_PATH
	}
	
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
	
	public boolean equals(Object o) {
		if (!(o instanceof LocationFacet))
			return false;
		LocationFacet lr = (LocationFacet) o;
		return lr.timestampMs == timestampMs && lr.latitude == this.latitude
				&& lr.longitude == this.longitude;
	}

	JsonCContent toContent() {
		JsonCContent content = new JsonCContent();
		content.data = this;
		return content;
	}

	@Override
	public int compareTo(LocationFacet o1) {
		return (o1.timestampMs > timestampMs)?-1:1;
	}

	@Override
	protected void makeFullTextIndexable() { }
}
