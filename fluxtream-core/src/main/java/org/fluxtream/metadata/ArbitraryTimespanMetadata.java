package org.fluxtream.metadata;

import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import org.fluxtream.TimeUnit;
import org.fluxtream.TimezoneMap;
import org.fluxtream.domain.metadata.VisitedCity;

/**
 * User: candide
 * Date: 02/10/13
 * Time: 16:25
 */
public class ArbitraryTimespanMetadata extends AbstractTimespanMetadata {

    public ArbitraryTimespanMetadata(final VisitedCity consensusVisitedCity, final VisitedCity previousInferredCity, final VisitedCity nextInferredCity,
                                     final TreeMap<String, TimeZone> consensusTimezones, final TimezoneMap timezoneMap,
                                     final List<VisitedCity> cities, List<VisitedCity> consensusCities, final long start, final long end) {
        super(consensusVisitedCity, previousInferredCity, nextInferredCity, consensusTimezones, timezoneMap, cities, consensusCities);
        this.start = start;
        this.end = end;
    }

    public ArbitraryTimespanMetadata(final long start, final long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.ARBITRARY;
    }

}
