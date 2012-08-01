package com.fluxtream.mvc.models;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.fluxtream.services.MetadataService;
import net.sf.json.JSONObject;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fluxtream.Configuration;
import com.fluxtream.TimeUnit;
import com.fluxtream.utils.TimeUtils;
import com.fluxtream.utils.Utils;

public class CalendarModel {

	public TimeUnit timeUnit = TimeUnit.DAY;

	private static final DateTimeFormatter currentDateFormatter = DateTimeFormat
			.forPattern("EEE, MMM d, yyyy");
	private static final DateTimeFormatter jsDateFormatter = DateTimeFormat
			.forPattern("yyyy-MM-dd");
	private static final DateTimeFormatter shortDayFormatter = DateTimeFormat
			.forPattern("MMM d");
	private static final DateTimeFormatter currentMonthFormatter = DateTimeFormat
			.forPattern("MMMMMMMMMMMMM yyyy");
	private static final DateTimeFormatter currentYearFormatter = DateTimeFormat
			.forPattern("yyyy");

	public Calendar fromCalendar;
	public Calendar toCalendar;

	private String title;

    public CalendarModel(long guestId, MetadataService metadataService) {
        final TimeZone currentTimeZone = metadataService.getTimeZone(guestId, System.currentTimeMillis());
        setToToday(TimeUnit.DAY, currentTimeZone);
    }

    public static CalendarModel fromState(long guestId, MetadataService metadataService, final String state) {
        CalendarModel calendarModel = new CalendarModel(guestId, metadataService);
        calendarModel.parseState(guestId, metadataService, state);
        return calendarModel;
    }

	public void setYear(final long guestId, final MetadataService metadataService, int year) {
        this.timeUnit = TimeUnit.YEAR;
		fromCalendar.set(Calendar.YEAR, year);
		fromCalendar.set(Calendar.MONTH, Calendar.JANUARY);
		fromCalendar.set(Calendar.DATE, 1);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.set(Calendar.YEAR, year);
		toCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
		toCalendar.set(Calendar.DATE, toCalendar.getActualMaximum(Calendar.DATE));
		toCalendar = TimeUtils.setToMidnight(fromCalendar);
	}

	public void setDate(final long guestId, final MetadataService metadataService, String formattedDate) {
        this.timeUnit = TimeUnit.DAY;
        final TimeZone tz = metadataService.getTimeZone(guestId, formattedDate);
        Date date = new Date(jsDateFormatter.withZone(
				DateTimeZone.forTimeZone(tz)).parseMillis(formattedDate));
		fromCalendar.setTime(date);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.setTime(date);
		toCalendar = TimeUtils.setToMidnight(fromCalendar);
	}

	public void setWeek(final long guestId, final MetadataService metadataService, int year, int week) {
        this.timeUnit = TimeUnit.WEEK;
		fromCalendar.set(Calendar.YEAR, year);
		fromCalendar.set(Calendar.WEEK_OF_YEAR, week);
		fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
		fromCalendar.set(Calendar.MINUTE, 0);
		fromCalendar.set(Calendar.SECOND, 0);
		fromCalendar.set(Calendar.MILLISECOND, 0);

		toCalendar.set(Calendar.YEAR, year);
		toCalendar.set(Calendar.WEEK_OF_YEAR, week + 1);
		toCalendar.set(Calendar.HOUR_OF_DAY, 23);
		toCalendar.set(Calendar.MINUTE, 59);
		toCalendar.set(Calendar.SECOND, 59);
		toCalendar.set(Calendar.MILLISECOND, 999);
	}

	public void setMonth(final long guestId, final MetadataService metadataService, int year, int month) {
        this.timeUnit = timeUnit.MONTH;
		fromCalendar.set(Calendar.YEAR, year);
		fromCalendar.set(Calendar.MONTH, month);
		fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
		fromCalendar.set(Calendar.MINUTE, 0);
		fromCalendar.set(Calendar.SECOND, 0);
		fromCalendar.set(Calendar.MILLISECOND, 0);

		toCalendar.set(Calendar.YEAR, year);
		toCalendar.set(Calendar.MONTH, month + 1);
		toCalendar.set(Calendar.HOUR_OF_DAY, 23);
		toCalendar.set(Calendar.MINUTE, 59);
		toCalendar.set(Calendar.SECOND, 59);
		toCalendar.set(Calendar.MILLISECOND, 999);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		try {
			this.title = new String(title.getBytes(), "utf-8");
		} catch (UnsupportedEncodingException e) {
		}
	}

	public long getStart() {
		return fromCalendar.getTimeInMillis();
	}

	public long getEnd() {
		return toCalendar.getTimeInMillis();
	}

	public String toJSONString(long guestId, MetadataService metadataService, Configuration env) {
		JSONObject json = new JSONObject();
		json.put("timeUnit", timeUnit.toString());
		json.put("currentTimespanLabel", timespanLabel());
		json.put("isToday", isToday(guestId, metadataService));
		json.put("state", getState());
        json.put("start", getStart());
        json.put("end", getEnd());
		if (this.title != null) {
			json.put("title", title);
		}
		return json.toString();
	}

	/**
	 * return a hash that serves as a client-side caching key; it is
	 * release-based
	 * 
	 * @param env
	 * @return
	 */
	private String getTimeHash(Configuration env, String configKey) {
		String toHash = env.get("release") + getStart() + getEnd()
				+ configKey;
		return Utils.hash(toHash);
	}

	private String getState() {
		if (timeUnit == TimeUnit.DAY)
			return "date/"
					+ jsDateFormatter.withZone(DateTimeZone.forTimeZone(fromCalendar.getTimeZone()))
							.print(fromCalendar.getTimeInMillis());
		else if (timeUnit == TimeUnit.WEEK)
			return "week/" + fromCalendar.get(Calendar.YEAR) + "/"
					+ fromCalendar.get(Calendar.WEEK_OF_YEAR);
		else if (timeUnit == TimeUnit.MONTH)
			return "month/" + fromCalendar.get(Calendar.YEAR) + "/"
					+ fromCalendar.get(Calendar.MONTH);
		else if (timeUnit == TimeUnit.YEAR)
			return "year/" + fromCalendar.get(Calendar.YEAR);
		return "UNKNOWN_DATE";
	}

	public void setToToday(TimeUnit timeUnit, TimeZone tz) {
        this.timeUnit = timeUnit;
		fromCalendar = TimeUtils.setFromMidnight(new GregorianCalendar(tz));
		toCalendar = TimeUtils.setToMidnight(new GregorianCalendar(tz));
        switch (timeUnit){
            case WEEK:
                this.timeUnit = TimeUnit.WEEK;
                break;
            case MONTH:
                this.timeUnit = TimeUnit.MONTH;
                break;
            case YEAR:
                this.timeUnit = TimeUnit.YEAR;
                break;
        }
	}

	public boolean isToday(long guestId, MetadataService metadataService) {
		if (timeUnit != TimeUnit.DAY)
			return false;
        final TimeZone currentUserTimeZone = metadataService.getTimeZone(guestId, System.currentTimeMillis());
        Calendar today = Calendar.getInstance(currentUserTimeZone);
		boolean result = fromCalendar.get(Calendar.YEAR) == today
				.get(Calendar.YEAR);
		result &= fromCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH);
		result &= fromCalendar.get(Calendar.DAY_OF_MONTH) == today
				.get(Calendar.DAY_OF_MONTH);
		return result;
	}

	private String timespanLabel() {
		String currentTimespanLabel = "";
		switch (this.timeUnit) {
		case DAY:
			currentTimespanLabel = currentDateFormatter.withZone(
					DateTimeZone.forTimeZone(fromCalendar.getTimeZone())).print(
					fromCalendar.getTimeInMillis());
			break;
		case WEEK:
			String from = shortDayFormatter.withZone(
					DateTimeZone.forTimeZone(fromCalendar.getTimeZone())).print(
					fromCalendar.getTimeInMillis());
			String to = shortDayFormatter
					.withZone(DateTimeZone.forTimeZone(toCalendar.getTimeZone())).print(
							toCalendar.getTimeInMillis());
			String year = currentYearFormatter.print(fromCalendar
					.getTimeInMillis());
			currentTimespanLabel = from + " - " + to + " " + year;
			break;
		case MONTH:
			currentTimespanLabel = currentMonthFormatter.withZone(
					DateTimeZone.forTimeZone(fromCalendar.getTimeZone())).print(
					fromCalendar.getTimeInMillis());
			break;
		case YEAR:
			currentTimespanLabel = currentYearFormatter.withZone(
					DateTimeZone.forTimeZone(fromCalendar.getTimeZone())).print(
					fromCalendar.getTimeInMillis());
			break;
		}

		return currentTimespanLabel;
	}

	public void incrementTimespan(final long guestId, final MetadataService metadataService, final String state) {
        parseState(guestId, metadataService, state);
		switch (this.timeUnit) {
		case DAY:
			if (!isToday(guestId, metadataService)) {
				fromCalendar.add(Calendar.DATE, 1);
				toCalendar.add(Calendar.DATE, 1);
			}
			break;
		case WEEK:
			fromCalendar.add(Calendar.WEEK_OF_YEAR, 1);
			toCalendar.add(Calendar.WEEK_OF_YEAR, 1);
			break;
		case MONTH:
			fromCalendar.add(Calendar.MONTH, 1);
			toCalendar.add(Calendar.MONTH, 1);
			break;
		case YEAR:
			fromCalendar.add(Calendar.YEAR, 1);
			toCalendar.add(Calendar.YEAR, 1);
			break;
		}
	}

	public void decrementTimespan(final long guestId, final MetadataService metadataService, final String state) {
        parseState(guestId, metadataService, state);
		switch (this.timeUnit) {
		case DAY:
			fromCalendar.add(Calendar.DATE, -1);
			toCalendar.add(Calendar.DATE, -1);
			break;
		case WEEK:
			fromCalendar.add(Calendar.WEEK_OF_YEAR, -1);
			toCalendar.add(Calendar.WEEK_OF_YEAR, -1);
			break;
		case MONTH:
			fromCalendar.add(Calendar.MONTH, -1);
			toCalendar.add(Calendar.MONTH, -1);
			break;
		case YEAR:
			fromCalendar.add(Calendar.YEAR, -1);
			toCalendar.add(Calendar.YEAR, -1);
			break;
		}
	}

    private void parseState(final long guestId, final MetadataService metadataService, final String state) {
        String[] stateParts = state.split("/");
        TimeUnit timeUnit = TimeUnit.fromValue(stateParts[0].equals("date")?"day":stateParts[0]);

        switch(timeUnit) {
            case DAY:
                setDate(guestId, metadataService, stateParts[1]);
                break;
            case WEEK:
                int year = Integer.valueOf(stateParts[1]);
                int week = Integer.valueOf(stateParts[2]);
                setWeek(guestId, metadataService, year, week);
                break;
            case MONTH:
                year = Integer.valueOf(stateParts[1]);
                int month = Integer.valueOf(stateParts[2]);
                setMonth(guestId, metadataService, year, month);
                break;
            case YEAR:
                year = Integer.valueOf(stateParts[1]);
                setYear(guestId, metadataService, year);
                break;
        }
    }

    public void setYearTimeUnit() {
        this.timeUnit = TimeUnit.YEAR;
        //fromCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
        fromCalendar.set(Calendar.MONTH, Calendar.JANUARY);
        fromCalendar.set(Calendar.DATE, 1);
        fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
        toCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
        toCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        toCalendar.set(Calendar.DAY_OF_MONTH,
                       toCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        toCalendar = TimeUtils.setToMidnight(toCalendar);
    }

    public void setMonthTimeUnit() {
        this.timeUnit = TimeUnit.MONTH;
        fromCalendar.set(Calendar.DAY_OF_MONTH, 1);
        fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
        toCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
        toCalendar.set(Calendar.MONTH, fromCalendar.get(Calendar.MONTH));
        toCalendar.set(Calendar.DAY_OF_MONTH,
                       toCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        toCalendar = TimeUtils.setToMidnight(toCalendar);
    }

    public void setDayTimeUnit() {
        timeUnit = TimeUnit.DAY;
        //fromCalendar.set(Calendar.DATE, 1);
        fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
        toCalendar.set(Calendar.DATE, fromCalendar.get(Calendar.DATE));
        toCalendar = TimeUtils.setToMidnight(toCalendar);
    }

    public void setWeekTimeUnit() {
        this.timeUnit = TimeUnit.WEEK;
        fromCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
        toCalendar.add(Calendar.WEEK_OF_YEAR, 1);
        toCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
        toCalendar.set(Calendar.MONTH, fromCalendar.get(Calendar.MONTH));
        toCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        toCalendar = TimeUtils.setToMidnight(toCalendar);
    }

}
