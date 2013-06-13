package com.fluxtream.metadata;

import java.util.List;
import java.util.TimeZone;
import com.fluxtream.OutsideTimeBoundariesException;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * User: candide
 * Date: 30/05/13
 * Time: 10:39
 */
public abstract class AbstractTimespanMetadata {

    public float maxTempC = -10000,
            maxTempF = -10000,
            minTempC = 10000,
            minTempF = 10000;

    public long start, end;

    public VisitedCity consensusVisitedCity;
    public VisitedCity nextInferredCity;
    public VisitedCity previousInferredCity;

    public List<VisitedCity> cities;

    protected static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    protected class TimespanTimeInterval implements TimeInterval {

        @Override
        public TimeZone getMainTimeZone() {
            return TimeZone.getTimeZone(consensusVisitedCity.city.geo_timezone);
        }

        @Override
        public long getStart() { return start; };

        @Override
        public long getEnd() { return end; }

        @Override
        public TimeUnit getTimeUnit() { return getTimespanTimeUnit(); }

        @Override
        public TimeZone getTimeZone(final long time) throws OutsideTimeBoundariesException {
            if (getTimeUnit()==TimeUnit.DAY)
                return getMainTimeZone();
            for (VisitedCity city : cities) {
                long dayStart = city.getDayStart();
                long dayEnd = city.getDayEnd();
                if (dayStart<time&&dayEnd>time)
                    return TimeZone.getTimeZone(city.city.geo_timezone);
            }
            throw new OutsideTimeBoundariesException();
        }

    }

    public AbstractTimespanMetadata() {}

    AbstractTimespanMetadata(List<VisitedCity> cities, VisitedCity consensusVisitedCity, VisitedCity previousInferredCity, VisitedCity nextInferredCity) {
        this.cities = cities;
        this.consensusVisitedCity = consensusVisitedCity;
        this.nextInferredCity = nextInferredCity;
        this.previousInferredCity = previousInferredCity;
    }

    protected long getTimeForDate(final VisitedCity consensusVisitedCity, final String forDate) {
        final DateTimeZone dateTimeZone = DateTimeZone.forID(consensusVisitedCity.city.geo_timezone);
        long forDateTime = formatter.withZone(dateTimeZone).parseDateTime(forDate).getMillis();
        start = forDateTime;
        return forDateTime;
    }

    protected abstract TimeUnit getTimespanTimeUnit();

    public TimeInterval getTimeInterval() {
        return new TimespanTimeInterval();
    }

}
