package org.fluxtream.core.domain.metadata;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.utils.TimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

/**
 * User: candide
 * Date: 28/04/13
 * Time: 10:31
 */
@Entity(name="Facet_VisitedCity")
@NamedQueries( {
       @NamedQuery( name="visitedCities.delete.all",
                    query="DELETE FROM Facet_VisitedCity cities WHERE cities.guestId=? "),
       @NamedQuery( name="visitedCities.byApiDateAndCity",
                    query="SELECT facet from Facet_VisitedCity facet WHERE facet.guestId=? AND facet.apiKeyId=? AND facet.date=? AND facet.city.id=? "),
       @NamedQuery( name="visitedCities.byApiAndTime",
                    query="SELECT facet from Facet_VisitedCity facet WHERE facet.apiKeyId=? AND facet.start<=? AND facet.end>=? ")
})
public class VisitedCity extends AbstractLocalTimeFacet implements Comparable<VisitedCity>{

    public LocationFacet.Source locationSource;

    public int sunrise;
    public int sunset;

    public long count;

    public transient int daysInferred;

    private transient DateTime dateTime;

    @ManyToOne(fetch= FetchType.EAGER, targetEntity = City.class, optional=false)
    public City city;

    public VisitedCity() {}

    public VisitedCity(final VisitedCity otherCity) {
        this.setId(otherCity.getId());
        this.locationSource = otherCity.locationSource;
        this.timeUpdated = otherCity.timeUpdated;
        this.api = otherCity.api;
        this.apiKeyId = otherCity.apiKeyId;
        this.date = otherCity.date;
        this.start = otherCity.start;
        this.end = otherCity.end;
        this.startTimeStorage = otherCity.startTimeStorage;
        this.endTimeStorage = otherCity.endTimeStorage;
        this.sunrise = otherCity.sunrise;
        this.sunset = otherCity.sunset;
        this.city = otherCity.city;
        this.count = otherCity.count;
        this.daysInferred = otherCity.daysInferred;
    }

    public VisitedCity(long apiKeyId) {
        super(apiKeyId);
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public long getDayStart() {
        final DateTime dateTime = getDateTime();
        return dateTime.getMillis();
    }

    public long getDayEnd() {
        final DateTime dateTime = getDateTime();
        return dateTime.getMillis()+ DateTimeConstants.MILLIS_PER_DAY;
    }

    private DateTime getDateTime() {
        if (dateTime==null)
            dateTime = TimeUtils.dateFormatter.withZone(DateTimeZone.forID(city.geo_timezone)).parseDateTime(date);
        return dateTime;
    }

    @Override
    protected void makeFullTextIndexable() {
        if (this.city!=null)
            this.fullTextDescription = city.geo_name;
    }

    @Override
    public int compareTo(final VisitedCity o) {
        int dateComparison = this.date.compareTo(o.date);
        if (dateComparison==0) {
            return (int)(start-o.start);
        } else
            return dateComparison;
    }
}
