package com.fluxtream.metadata;

import java.util.List;
import com.fluxtream.TimeInterval;
import com.fluxtream.domain.metadata.VisitedCity;
import org.joda.time.DateTimeZone;
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

    public VisitedCity consensusVisitedCity;
    public VisitedCity nextInferredCity;
    public VisitedCity previousInferredCity;

    public List<VisitedCity> cities;

    protected static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    public AbstractTimeUnitMetadata() {}

    AbstractTimeUnitMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity,
                             VisitedCity previousInferredCity, VisitedCity nextInferredCity) {
        this.cities = cities;
        this.consensusVisitedCity = consensusVisitedCity;
        this.nextInferredCity = nextInferredCity;
        this.previousInferredCity = previousInferredCity;
    }

    protected long getTimeForDate(final VisitedCity consensusVisitedCity, final String forDate) {
        final DateTimeZone dateTimeZone = DateTimeZone.forID(consensusVisitedCity.city.geo_timezone);
        long forDateTime = formatter.withZone(dateTimeZone).parseDateTime(forDate).getMillis();
        timeZone = consensusVisitedCity.city.geo_timezone;
        start = forDateTime;
        return forDateTime;
    }

    @Deprecated
    public abstract TimeInterval getTimeInterval();

}
