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
public class VisitedCity extends AbstractLocalTimeFacet {

    @Index(name="locationSource_index")
    public LocationFacet.Source locationSource;

    public int sunrise;
    public int sunset;

    public long count;

    public boolean isConsensus() {
        return apiKeyId==0;
    }

    @ManyToOne(fetch= FetchType.EAGER, targetEntity = City.class, optional=false)
    public City city;

    // for consensus, make apiKeyId=0
    public VisitedCity(long apiKeyId) {
        super(apiKeyId);
    }

    public VisitedCity() {
    }

    @Override
    protected void makeFullTextIndexable() {
        if (this.city!=null)
            this.fullTextDescription = city.geo_name;
    }

}
