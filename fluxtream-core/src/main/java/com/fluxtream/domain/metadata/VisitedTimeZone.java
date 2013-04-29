package com.fluxtream.domain.metadata;

import javax.persistence.Entity;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.domain.AbstractLocalTimeFacet;
import org.hibernate.annotations.Index;

/**
 * User: candide
 * Date: 28/04/13
 * Time: 10:31
 */
@Entity(name="Facet_VisitedTimezone")
public class VisitedTimeZone extends AbstractLocalTimeFacet {

    @Index(name="locationSource_index")
    public LocationFacet.Source locationSource;

    @Index(name="timezoneID_index")
    public String timezoneID;

    @Index(name="timezoneOffset_index")
    public int offsetInMinutes;

    public VisitedTimeZone() {
        this.apiKeyId = 1l;
    }

    @Override
    protected void makeFullTextIndexable() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
