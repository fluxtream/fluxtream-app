package com.fluxtream.domain.metadata;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.annotations.Index;
import org.hibernate.search.annotations.Indexed;

/**
 * User: candide
 * Date: 28/04/13
 * Time: 10:31
 */
@Entity(name="Facet_VisitedCity")
@Indexed
@NamedQueries( {
       @NamedQuery( name="visitedCities.byApiDateAndCity",
                    query="SELECT facet from Facet_VisitedCity facet WHERE facet.apiKeyId=? AND facet.date=? AND facet.city.id=?")
})
public class VisitedCity extends AbstractLocalTimeFacet implements Comparable<VisitedCity>{

    @Index(name="locationSource_index")
    public LocationFacet.Source locationSource;

    public int sunrise;
    public int sunset;

    public long count;

    public Byte mainCityBitPattern;

    public transient int daysInferred;

    @ManyToOne(fetch= FetchType.EAGER, targetEntity = City.class, optional=false)
    public City city;

    public VisitedCity() {}

    public VisitedCity(long apiKeyId) {
        super(apiKeyId);
    }

    @Override
    protected void makeFullTextIndexable() {
        if (this.city!=null)
            this.fullTextDescription = city.geo_name;
    }

    @Override
    public int compareTo(final VisitedCity o) {
        return (int)(start-o.start);
    }

}
