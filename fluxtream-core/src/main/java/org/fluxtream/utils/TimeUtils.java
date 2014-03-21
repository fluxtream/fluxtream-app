package org.fluxtream.utils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeUtils {

	private static final long MILLIS_IN_DAY = 86400000l;
	public static final int FIRST_DAY_OF_WEEK = DateTimeConstants.SUNDAY;
    public static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
    public static final DateTimeFormatter dateFormatterUTC = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC();

	public static String getStart(String date, TimeZone tz) {
		return date + " 00:00:00 " + tz.getDisplayName(true, TimeZone.SHORT);
	}
	
	public static String getEnd(String date, TimeZone tz) {
		return date + " 23:59:59 " + tz.getDisplayName(true, TimeZone.SHORT);
	}
	
	public static long time(String s) throws ParseException {
		DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss z");
	    long time = formatter.parseMillis(s);
	    return time;
	}
	
	public static final long fromMidnight(long time, TimeZone tz) {
		Calendar c = Calendar.getInstance(tz);
		c.setTimeInMillis(time);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTimeInMillis();
	}
	
	public static final long toMidnight(long time, TimeZone tz) {
		return toMidnight(time, 0, tz);
	}
	
	public static final long toMidnight(long time, int nDays, TimeZone tz) {
		Calendar c = Calendar.getInstance(tz);
		c.setTimeInMillis(time + nDays*MILLIS_IN_DAY);
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.MILLISECOND, 999);
		return c.getTimeInMillis();
	}

	public static LocalDate getBeginningOfWeek(final int year, final int week) {
		return (new LocalDate())
				.withWeekyear(year)
				.withWeekOfWeekyear(week)
				.minusWeeks(1)
				.withDayOfWeek(FIRST_DAY_OF_WEEK);
		// Need to subtract 1 week because Sunday is the last day of the week, which
		// would logically move all the week start dates forward by 6 days.  Better
		// one day earlier than JodaTime's standard than 6 days later than
		// users' expectations.
	}

    public static LocalDate getEndOfMonth(final int year, final int month) {
        return (new LocalDate())
                .withWeekyear(year)
                .withMonthOfYear(month).dayOfMonth().withMaximumValue();
    }

    public static LocalDate getBeginningOfMonth(final int year, final int month) {
        return (new LocalDate())
                .withWeekyear(year)
                .withMonthOfYear(month).dayOfMonth().withMinimumValue();
    }

}