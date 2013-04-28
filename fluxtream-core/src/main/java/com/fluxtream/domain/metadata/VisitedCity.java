package com.fluxtream.domain.metadata;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
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
public class VisitedCity extends AbstractLocalTimeFacet {

    public int sunrise;
    public int sunset;

    @Index(name="locationSource_index")
    LocationFacet.Source locationSource;

    @ManyToOne(fetch= FetchType.EAGER, targetEntity = City.class, optional=false)
    public City city;

    public VisitedCity() {
        this.apiKeyId = 1l;
    }

    @Override
    protected void makeFullTextIndexable() {
        if (this.city!=null)
            this.fullTextDescription = city.geo_name;
    }

}
