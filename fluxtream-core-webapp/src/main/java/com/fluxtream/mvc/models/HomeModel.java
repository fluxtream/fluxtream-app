package com.fluxtream.mvc.models;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import net.sf.json.JSONObject;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.utils.RandomString;
import com.fluxtream.utils.TimeUtils;
import com.fluxtream.utils.Utils;

@Component
@Scope(value = "prototype")
public class HomeModel {

	@Autowired
	ApiDataService apiDataService;

	private TimeZone tz;
	private VisualizationType viewType;
	private VisualizationType dayVisualizationType;
	private VisualizationType aggregatedVisualizationType;
	List<String> uncheckedConnectors;

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
	public boolean forceUpdate;
	@SuppressWarnings("unused")
	private final static RandomString randomString = new RandomString(64);

	private String title;

	public String getDate() {
		return jsDateFormatter.withZone(DateTimeZone.forTimeZone(this.tz))
				.print(this.getStart());
	}

	public void init(String timeZone) {
		setTimeZone(TimeZone.getTimeZone(timeZone));
		this.viewType = VisualizationType.CLOCK;
		this.dayVisualizationType = viewType;
		this.aggregatedVisualizationType = VisualizationType.STATS;
		setToToday();
	}

	public void setTimeZone(TimeZone tz) {
		this.tz = tz;
		if (fromCalendar != null)
			fromCalendar.setTimeZone(tz);
		if (toCalendar != null)
			toCalendar.setTimeZone(tz);
	}

	public List<String> getUncheckedConnectors() {
		if (uncheckedConnectors == null)
			uncheckedConnectors = new ArrayList<String>();
		return uncheckedConnectors;
	}

	public void setUncheckedConnectors(String[] connectorNames) {
		if (uncheckedConnectors == null)
			uncheckedConnectors = new ArrayList<String>();
		for (String connectorName : connectorNames) {
			if (!uncheckedConnectors.contains(connectorName))
				uncheckedConnectors.add(connectorName);
		}
	}

	public void setCheckedConnectors(String[] connectorNames) {
		if (uncheckedConnectors == null)
			uncheckedConnectors = new ArrayList<String>();
		for (String connectorName : connectorNames) {
			if (uncheckedConnectors.contains(connectorName))
				uncheckedConnectors.remove(connectorName);
		}
	}

	public TimeInterval getTimeInterval() {
		return new TimeInterval(this.fromCalendar.getTimeInMillis(),
				this.toCalendar.getTimeInMillis(), this.timeUnit, tz);
	}

	public TimeZone getTimeZone() {
		return tz;
	}

	public void setYear(int year) {
		setYearTimeUnit();
		fromCalendar.set(Calendar.YEAR, year);
		fromCalendar.set(Calendar.MONTH, Calendar.JANUARY);
		fromCalendar.set(Calendar.DATE, 1);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.set(Calendar.YEAR, year);
		toCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
		toCalendar.set(Calendar.DATE, toCalendar.getActualMaximum(Calendar.DATE));
		toCalendar = TimeUtils.setToMidnight(fromCalendar);
	}

	public void setDate(String formattedDate) {
		setDayTimeUnit();
		Date date = new Date(jsDateFormatter.withZone(
				DateTimeZone.forTimeZone(tz)).parseMillis(formattedDate));
		fromCalendar.setTime(date);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.setTime(date);
		toCalendar = TimeUtils.setToMidnight(fromCalendar);
	}

	public void setWeek(int year, int week) {
		setWeekTimeUnit();
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

	public void setMonth(int year, int month) {
		setMonthTimeUnit();
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

	public VisualizationType getViewType() {
		return this.viewType;
	}
	
	public String toJSONString(Configuration env, String configKey) {
		JSONObject json = new JSONObject();
		json.put("visualizationType", viewType.toString());
		json.put("timeUnit", timeUnit.toString());
		json.put("currentTimespanLabel", timespanLabel());
		json.put("isToday", isToday());
		json.put("state", getUrlDate());
		json.put("timeHash", getTimeHash(env, configKey));
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

	private String getUrlDate() {
		if (timeUnit == TimeUnit.DAY)
			return "date/"
					+ jsDateFormatter.withZone(DateTimeZone.forTimeZone(tz))
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

	public void setToToday() {
		fromCalendar = TimeUtils.setFromMidnight(new GregorianCalendar(tz));
		toCalendar = TimeUtils.setToMidnight(new GregorianCalendar(tz));
		timeUnit = TimeUnit.DAY;
		viewType = dayVisualizationType;
	}

	public boolean isToday() {
		if (timeUnit != TimeUnit.DAY)
			return false;
		Calendar today = Calendar.getInstance(tz);
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
					DateTimeZone.forTimeZone(tz)).print(
					fromCalendar.getTimeInMillis());
			break;
		case WEEK:
			String from = shortDayFormatter.withZone(
					DateTimeZone.forTimeZone(tz)).print(
					fromCalendar.getTimeInMillis());
			String to = shortDayFormatter
					.withZone(DateTimeZone.forTimeZone(tz)).print(
							toCalendar.getTimeInMillis());
			String year = currentYearFormatter.print(fromCalendar
					.getTimeInMillis());
			currentTimespanLabel = from + " - " + to + " " + year;
			break;
		case MONTH:
			currentTimespanLabel = currentMonthFormatter.withZone(
					DateTimeZone.forTimeZone(tz)).print(
					fromCalendar.getTimeInMillis());
			break;
		case YEAR:
			currentTimespanLabel = currentYearFormatter.withZone(
					DateTimeZone.forTimeZone(tz)).print(
					fromCalendar.getTimeInMillis());
			break;
		}

		return currentTimespanLabel;
	}

	public void setViewType(VisualizationType vt) {
		this.viewType = vt;
		switch (vt) {
		case CLOCK:
		case TIMELINE:
			this.dayVisualizationType = vt;
			break;
		case STATS:
		case LIST:
			this.dayVisualizationType = vt;
			this.aggregatedVisualizationType = vt;
		}
	}

	public void incrementTimespan(final String state) {
        syncState(state);
		switch (this.timeUnit) {
		case DAY:
			if (!isToday()) {
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

	public void decrementTimespan(final String state) {
        syncState(state);
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

    private void syncState(final String state) {
        String[] stateParts = state.split("/");
        TimeUnit timeUnit = TimeUnit.fromValue(stateParts[0].equals("date")?"day":stateParts[0]);
        switch(timeUnit) {
            case DAY:
                setDate(stateParts[1]);
                break;
            case WEEK:
                int year = Integer.valueOf(stateParts[1]);
                int week = Integer.valueOf(stateParts[2]);
                setWeek(year, week);
                break;
            case MONTH:
                year = Integer.valueOf(stateParts[1]);
                int month = Integer.valueOf(stateParts[2]);
                setMonth(year, month);
                break;
            case YEAR:
                year = Integer.valueOf(stateParts[1]);
                setYear(year);
                break;
        }
    }


    public void setYearTimeUnit() {
		this.timeUnit = TimeUnit.YEAR;
		fromCalendar.set(Calendar.YEAR, fromCalendar.get(Calendar.YEAR));
		fromCalendar.set(Calendar.MONTH, Calendar.JANUARY);
        fromCalendar.set(Calendar.DATE, 1);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.set(Calendar.YEAR, toCalendar.get(Calendar.YEAR));
		toCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
		toCalendar.set(Calendar.DAY_OF_MONTH,
				toCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		toCalendar = TimeUtils.setToMidnight(toCalendar);
		this.viewType = this.aggregatedVisualizationType;
	}

	public void setMonthTimeUnit() {
		Calendar c = Calendar.getInstance(tz);
		this.timeUnit = TimeUnit.MONTH;
		fromCalendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
		fromCalendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
		fromCalendar.set(Calendar.DAY_OF_MONTH, 1);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
		toCalendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
		toCalendar.set(Calendar.DAY_OF_MONTH,
				toCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		toCalendar = TimeUtils.setToMidnight(toCalendar);
		this.viewType = this.aggregatedVisualizationType;
	}

	public void setDayTimeUnit() {
        timeUnit = TimeUnit.DAY;
        //fromCalendar.set(Calendar.DATE, 1);
        fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.set(Calendar.DATE, fromCalendar.get(Calendar.DATE));
		toCalendar = TimeUtils.setToMidnight(toCalendar);
		this.viewType = this.dayVisualizationType;
	}

	public void setWeekTimeUnit() {
		Calendar c = Calendar.getInstance(tz);
		this.timeUnit = TimeUnit.WEEK;
		fromCalendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
		fromCalendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
		fromCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		fromCalendar = TimeUtils.setFromMidnight(fromCalendar);
		toCalendar.add(Calendar.WEEK_OF_YEAR, 1);
		toCalendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
		toCalendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
		toCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		toCalendar = TimeUtils.setToMidnight(toCalendar);
		this.viewType = this.aggregatedVisualizationType;
	}

}
