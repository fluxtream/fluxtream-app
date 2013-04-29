package com.fluxtream.domain.metadata;

import javax.persistence.Entity;
import com.fluxtream.domain.AbstractLocalTimeFacet;

/**
 * User: candide
 * Date: 29/04/13
 * Time: 13:15
 */
@Entity(name="Facet_SunriseSunset")
public class SunriseSunset extends AbstractLocalTimeFacet {

    public SunriseSunset() { this.apiKeyId = 1l; }

    public int sunrise;
    public int sunset;

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
