package org.fluxtream.mvc.models;

import java.util.TimeZone;
import org.fluxtream.Configuration;
import org.fluxtream.TimeUnit;
import org.fluxtream.services.MetadataService;
import org.fluxtream.utils.TimeUtils;
import org.fluxtream.utils.Utils;
import net.sf.json.JSONObject;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class CalendarModel {

    private static final DateTimeFormatter currentDateFormatter = DateTimeFormat
            .forPattern("EEE, MMM d, yyyy");
    private static final DateTimeFormatter shortDayFormatter = DateTimeFormat
            .forPattern("MMM d");
    private static final DateTimeFormatter currentMonthFormatter = DateTimeFormat
            .forPattern("MMMMMMMMMMMMM yyyy");
    private static final DateTimeFormatter currentYearFormatter = DateTimeFormat
            .forPattern("yyyy");

    private final long guestId;
    private final MetadataService metadataService;

    private TimeUnit timeUnit = TimeUnit.DAY;
    private LocalDate fromDate;

    public CalendarModel(final long guestId, final MetadataService metadataService) {
        this.guestId = guestId;
        this.metadataService = metadataService;
        setToToday();
    }

    public static CalendarModel fromState(final long guestId,
                                          final MetadataService metadataService,
                                          final String state) {
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.replaceState(state);
        return calendarModel;
    }

    public void setYear(final int year) {
        timeUnit = TimeUnit.YEAR;
        fromDate = new LocalDate(year, 1, 1);
    }

    public void setDate(final String formattedDate) {
        this.timeUnit = TimeUnit.DAY;
        // TODO: If we were using JodaTime 2.0 or later, we could simply write
        // fromDate = LocalDate.parse(formattedDate, jsDateFormatter)
        fromDate = TimeUtils.dateFormatter.parseDateTime(formattedDate).toLocalDate();
    }

    public void setWeek(final int year, final int week) {
        this.timeUnit = TimeUnit.WEEK;
        fromDate = TimeUtils.getBeginningOfWeek(year, week);
    }

    public int getWeekYear() {
        if (timeUnit != TimeUnit.WEEK)
            throw new IllegalStateException("Unexpected check for week year when not using week time unit");
        // Off by 1 because getBeginningOfWeek(year, week), and by extension
        // setWeek(year, week) goes back by 1 week
        return fromDate.plusWeeks(1).getWeekyear();
    }

    public int getWeek() {
        if (timeUnit != TimeUnit.WEEK)
            throw new IllegalStateException("Unexpected check for week when not using week time unit");
        // Off by 1 because getBeginningOfWeek(year, week), and by extension
        // setWeek(year, week) goes back by 1 week
        return fromDate.plusWeeks(1).getWeekOfWeekyear();
    }

    public void setMonth(final int year, final int month) {
        timeUnit = timeUnit.MONTH;
        fromDate = new LocalDate(year, 1, 1).withMonthOfYear(month);
    }

    /**
     * Returns one past the end date.
     *
     * This is similar to the behavior of Python's range function or Java's String.substring method,
     * and it has the desirable property that the number of milliseconds between midnight on
     * getToDate() and midnight on fromDate is equal to the number of milliseconds in a day times
     * the number of days in the range described by TimeUnit (assuming there is no time zone change,
     * leap second, or daylight savings time start/end in the middle).
     */
    private LocalDate getToDate() {
        switch (timeUnit) {
            case DAY:
                return fromDate.plusDays(1);
            case WEEK:
                return fromDate.plusWeeks(1);
            case MONTH:
                return fromDate.plusMonths(1);
            case YEAR:
                return fromDate.plusYears(1);
        }

        throw new UnsupportedOperationException("Unexpected TimeUnit value");
    }

    private TimeZone getTimeZone(final LocalDate date) {
        // The format used for dateString is the same format used in
        // MetadataServiceImpl's formatter
        final String dateString = date.toString("yyyy-MM-dd");
        return metadataService.getTimeZone(guestId, dateString);
    }

    private DateTimeZone getBrowserDateTimeZone() {
        // TODO: This doesn't match the implementation, even though it should be right
        final TimeZone tz = metadataService.getCurrentTimeZone(guestId);
        return DateTimeZone.forTimeZone(tz);
    }

    private long getMillis(final LocalDate date) {
        return date.toDateTimeAtStartOfDay(DateTimeZone.forTimeZone(getTimeZone(date)))
                .getMillis();
    }

    private long getMillisAtTrailingMidnight(final LocalDate date){
        return getMillis(date) + DateTimeConstants.MILLIS_PER_DAY;
    }

    public long getStart() {
        return getMillis(fromDate);
    }

    public long getEnd() {
        return getMillisAtTrailingMidnight(getToDate().minusDays(1));
    }

    public String toJSONString(final Configuration env) {
        // TODO: Include information in JSON that tells which calendar cells should
        // be lit up - don't want to use UTC timestamps to determine that info

        final JSONObject json = new JSONObject();
        json.put("timeUnit", timeUnit.toString());
        json.put("currentTimespanLabel", timespanLabel());
        json.put("isToday", isToday());
        json.put("state", getState());
        json.put("start", getStart());
        json.put("end", getEnd());
        return json.toString();
    }

    /**
     * Returns a hash that serves as a client-side caching key; it is
     * release-based
     */
    private String getTimeHash(final Configuration env, final String configKey) {
        final String toHash = env.get("release") + getStart() + getEnd() + configKey;
        return Utils.hash(toHash);
    }

    private String getState() {
        switch (timeUnit) {
            case DAY:
                return "date/" + TimeUtils.dateFormatter.print(fromDate);
            case WEEK:
                return String.format("week/%d/%d", getWeekYear(), getWeek());
            case MONTH:
                return String.format("month/%d/%d", fromDate.getYear(), fromDate.getMonthOfYear());
            case YEAR:
                return String.format("year/%d", fromDate.getYear());
        }

        throw new UnsupportedOperationException("Unexpected TimeUnit value");
    }

    public void setToToday() {
        timeUnit = TimeUnit.DAY;
        fromDate = new LocalDate(getBrowserDateTimeZone());
    }

    private boolean isToday() {
        final LocalDate today = new LocalDate(getBrowserDateTimeZone());
        return (timeUnit == TimeUnit.DAY)
               && (fromDate.getYear() == today.getYear())
               && (fromDate.getMonthOfYear() == today.getMonthOfYear())
               && (fromDate.getDayOfMonth() == today.getDayOfMonth());
    }

    private String timespanLabel() {
        switch (timeUnit) {
            case DAY:
                return currentDateFormatter.print(fromDate);
            case WEEK:
                final String from = shortDayFormatter.print(fromDate);
                final String to = shortDayFormatter.print(getToDate().minusDays(1));
                // TODO: Way to handle from and to in different years?
                final String year = currentYearFormatter.print(fromDate);
                return from + " - " + to + " " + year;
            case MONTH:
                return currentMonthFormatter.print(fromDate);
            case YEAR:
                return currentYearFormatter.print(fromDate);
        }

        throw new UnsupportedOperationException("Unexpected TimeUnit value");
    }

    public void incrementTimespan() {
        switch (timeUnit) {
            case DAY:
                fromDate = fromDate.plusDays(1);
                break;
            case WEEK:
                fromDate = fromDate.plusWeeks(1);
                break;
            case MONTH:
                fromDate = fromDate.plusMonths(1);
                break;
            case YEAR:
                fromDate = fromDate.plusYears(1);
                break;
        }
    }

    public void decrementTimespan() {
        switch (timeUnit) {
            case DAY:
                fromDate = fromDate.minusDays(1);
                break;
            case WEEK:
                fromDate = fromDate.minusWeeks(1);
                break;
            case MONTH:
                fromDate = fromDate.minusMonths(1);
                break;
            case YEAR:
                fromDate = fromDate.minusYears(1);
                break;
        }
    }

    public void replaceState(final String state) {
        final String[] stateParts = state.split("/");
        final TimeUnit timeUnit = TimeUnit.fromValue(stateParts[0].equals("date") ? "day" : stateParts[0]);

        final int year, month, week;

        switch (timeUnit) {
            case DAY:
                setDate(stateParts[1]);
                break;
            case WEEK:
                year = Integer.valueOf(stateParts[1]);
                week = Integer.valueOf(stateParts[2]);
                setWeek(year, week);
                break;
            case MONTH:
                year = Integer.valueOf(stateParts[1]);
                month = Integer.valueOf(stateParts[2]);
                setMonth(year, month);
                break;
            case YEAR:
                year = Integer.valueOf(stateParts[1]);
                setYear(year);
                break;
        }
    }

    public void setYearTimeUnit() {
        timeUnit = TimeUnit.YEAR;
        fromDate = new LocalDate(fromDate.getYear(), 1, 1);
    }

    public void setMonthTimeUnit() {
        timeUnit = TimeUnit.MONTH;
        fromDate = new LocalDate(fromDate.getYear(), fromDate.getMonthOfYear(), 1);
    }

    public void setDayTimeUnit() {
        timeUnit = TimeUnit.DAY;
        fromDate = new LocalDate(fromDate.getYear(), fromDate.getMonthOfYear(), fromDate.getDayOfMonth());
    }

    public void setWeekTimeUnit() {
        LocalDate origFromDate = fromDate;

        timeUnit = TimeUnit.WEEK;
        fromDate = new LocalDate(fromDate.getYear(), fromDate.getMonthOfYear(), fromDate.getDayOfMonth())
                .withDayOfWeek(TimeUtils.FIRST_DAY_OF_WEEK);
        // Unfortunately, the above code returns the following week instead of the containing week for
        // every day other than Sunday.  Check for this and decrement the week if needed.
        if(fromDate.isAfter(origFromDate)) {
            fromDate = fromDate.minusWeeks(1);
        }
    }
}