package com.fluxtream.connectors.location;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.ObjectTypeSpec;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
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
    		OTHER, USER, GOOGLE_LATITUDE, GEO_IP_DB, IP_TO_LOCATION, OPEN_PATH, RUNKEEPER, FLUXTREAM_CAPTURE, FLICKR, MOVES, MYMEE, NONE
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

    public static AbstractFacet getLatestFacet(EntityManager em, ApiKey apiKey, ObjectType objType){
        Source source = null;
        final String connectorName = apiKey.getConnector().getName();
        if (connectorName.equals("google_latitude"))
            source = Source.GOOGLE_LATITUDE;
        else if (connectorName.equals("moves"))
            source = Source.MOVES;
        else if (connectorName.equals("runkeeper"))
            source = Source.RUNKEEPER;
        return getOldestOrLatestFacet(em, apiKey, objType, source, "desc");
    }

    private static AbstractFacet getOldestOrLatestFacet(EntityManager em, ApiKey apiKey, ObjectType objType, final Source source, String sortOrder) {
        Class facetClass;
        if (objType != null) {
            facetClass = objType.facetClass();
        }
        else {
            facetClass = apiKey.getConnector().facetClass();
        }
        Entity entity = (Entity)facetClass.getAnnotation(Entity.class);
        Query query;
        if (source!=null)
            query = em.createQuery("select facet from " + entity.name()
                         + " facet where facet.guestId = "
                         + apiKey.getGuestId() + " and facet.source="
                         + source.ordinal() + " order by facet.end "
                         + sortOrder + " limit 1");
        else
            query = em.createQuery("select facet from " + entity.name()
                                   + " facet where facet.guestId = "
                                   + apiKey.getGuestId() + " order by facet.end "
                                   + sortOrder + " limit 1");
        query.setMaxResults(1);
        final List resultList = query.getResultList();
        if (resultList != null && resultList.size() > 0) {
            return (AbstractFacet)resultList.get(0);
        }
        return null;
    }


    @Override
	protected void makeFullTextIndexable() { }
}
