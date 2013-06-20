package com.fluxtream.metadata;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.fluxtream.TimeUnit;
import com.fluxtream.domain.metadata.VisitedCity;
import com.fluxtream.utils.TimeUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

/**
 * User: candide
 * Date: 29/05/13
 * Time: 18:04
 */
public class WeekMetadata extends AbstractTimespanMetadata {

    public WeekMetadata(final int year, final int week) {
        final LocalDate beginningOfWeek = TimeUtils.getBeginningOfWeek(year, week);
        this.start = beginningOfWeek.toDateTimeAtStartOfDay().getMillis();
        final LocalDate endOfWeek = beginningOfWeek.plusDays(7);
        this.end = endOfWeek.toDateTimeAtStartOfDay().getMillis() + DateTimeConstants.MILLIS_PER_DAY;
    }

    public WeekMetadata(final List<VisitedCity> cities, final VisitedCity consensusVisitedCity,
                        VisitedCity previousInferredCity, VisitedCity nextInferredCity) {
        super(cities, consensusVisitedCity, previousInferredCity, nextInferredCity);
        Collections.sort(cities, new Comparator<VisitedCity>() {
            @Override
            public int compare(final VisitedCity o1, final VisitedCity o2) {
                return (int)(o1.start - o2.start);
            }
        });

        this.start = cities.get(0).getDayStart();
        this.end = cities.get(cities.size()-1).getDayEnd();
    }

    @Override
    protected TimeUnit getTimespanTimeUnit() {
        return TimeUnit.WEEK;
    }

}
