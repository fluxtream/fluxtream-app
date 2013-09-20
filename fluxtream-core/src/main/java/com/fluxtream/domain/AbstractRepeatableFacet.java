package com.fluxtream.domain;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.MappedSuperclass;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * User: candide
 * Date: 20/09/13
 * Time: 19:27
 */
@MappedSuperclass
public abstract class AbstractRepeatableFacet extends AbstractFacet {

    public Date startDate;
    public Date endDate;

    public boolean allDayEvent;


    protected static final DateTimeFormatter formatter = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    public AbstractRepeatableFacet(final long apiKeyId) {
        super(apiKeyId);
    }

    public AbstractRepeatableFacet() {
        super();
    }

    public List<String> getRepeatedDates() {
        LocalDate currLocalDate = new LocalDate(startDate);
        final LocalDate endLocalDate = new LocalDate(endDate);
        List<String> dates = new ArrayList<String>();
        while(!currLocalDate.isAfter(endLocalDate)) {
            final String date = formatter.withZoneUTC().print(currLocalDate);
            dates.add(date);
            currLocalDate = currLocalDate.plusDays(1);
        }
        return dates;
    }

}
