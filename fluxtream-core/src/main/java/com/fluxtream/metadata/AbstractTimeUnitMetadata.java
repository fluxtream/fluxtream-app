package com.fluxtream.metadata;

import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.metadata.VisitedCity;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * User: candide
 * Date: 30/05/13
 * Time: 10:39
 */
public abstract class AbstractTimeUnitMetadata {

    public float maxTempC = -10000,
            maxTempF = -10000,
            minTempC = 10000,
            minTempF = 10000;

    public String timeZone = "UTC";
    public long start, end;
    public int daysInferred = 0;

    public VisitedCity consensusVisitedCity;
    public List<VisitedCity> cities;

    protected static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    public AbstractTimeUnitMetadata() {}

    AbstractTimeUnitMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity) {
        this.cities = cities;
        this.consensusVisitedCity = consensusVisitedCity;
    }

    protected long getTimeForDate(final VisitedCity consensusVisitedCity, final String forDate) {
        final DateTimeZone dateTimeZone = DateTimeZone.forID(consensusVisitedCity.city.geo_timezone);
        long cityTime = formatter.withZone(dateTimeZone).parseDateTime(consensusVisitedCity.date).getMillis();
        long forDateTime = formatter.withZone(dateTimeZone).parseDateTime(forDate).getMillis();
        daysInferred = Days.daysBetween(new DateMidnight(forDateTime), new DateMidnight(cityTime)).getDays();
        timeZone = consensusVisitedCity.city.geo_timezone;
        start = forDateTime;
        return forDateTime;
    }

    @Deprecated
    public abstract TimeInterval getTimeInterval();

}
