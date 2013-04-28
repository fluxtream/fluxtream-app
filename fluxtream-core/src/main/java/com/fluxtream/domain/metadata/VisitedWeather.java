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
 * Time: 10:34
 */
@Entity(name="Facet_VisitedWeather")
@Indexed
public class VisitedWeather extends AbstractLocalTimeFacet {

    @Index(name="locationSource_index")
    LocationFacet.Source locationSource;

    @ManyToOne(fetch= FetchType.EAGER, targetEntity = WeatherInfo.class, optional=false)
    WeatherInfo weatherInfo;

    public VisitedWeather() {
        this.apiKeyId = 1l;
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
