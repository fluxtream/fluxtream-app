package com.fluxtream.updaters.strategies;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.utils.TimeUtils;

@Component
public class AlwaysUpdateStrategy extends AbstractUpdateStrategy {

	@Autowired
	ConnectorUpdateService connectorUpdateService;

	/**
	 * Always refresh (no more than every noRefreshDelay milliseconds)
	 */
	@Override
	public UpdateInfo computeUpdateInfo(ApiKey apiKey, int objectTypes,
			TimeInterval timeInterval) {

		ApiUpdate lastUpdate = connectorUpdateService.getLastSuccessfulUpdate(apiKey);
		long now = System.currentTimeMillis();

		if (lastUpdate != null
				&& (now - lastUpdate.ts) < 15000)
			return UpdateInfo.noopUpdateInfo(apiKey, objectTypes);

		TimeInterval greedyInterval = getGreedyInterval(apiKey.getConnector(),
				timeInterval);

		if (timeInterval.isMostRecent())
			greedyInterval.end = now;

		return UpdateInfo.refreshTimeIntervalUpdateInfo(apiKey, objectTypes,
				greedyInterval);
	}

	/**
	 * Returns the maximum granularity timeInterval that yields timely results
	 * for the specified API (based on values in the global configuration)
	 * 
	 * @param api
	 * @param timeInterval
	 * @return
	 */
	final TimeInterval getGreedyInterval(Connector api,
			TimeInterval timeInterval) {
		String intervalKey = api.getName().toLowerCase() + ".intervalSize";
		String intervalSize = (String) env.connectors.getProperty(intervalKey);
		if (intervalSize == null)
			intervalSize = "DAY";
		TimeUnit granularity = TimeUnit.fromValue(intervalSize);
		long start = timeInterval.start;
		long end = timeInterval.end;
		switch (granularity) {
		case DAY:
			switch (timeInterval.timeUnit) {
			case DAY:
				return new TimeInterval(start, end, timeInterval.timeUnit,
						timeInterval.timeZone);
			case WEEK:
			case MONTH:
			case YEAR:
				throw new UnsuitableGranularityException(timeInterval.timeUnit,
						api);
			}
			break;
		case WEEK:
			switch (timeInterval.timeUnit) {
			case DAY:
			case WEEK:
				start = getBeginningOfWeek(start);
				end = getEndOfWeek(end);
				break;
			case MONTH:
			case YEAR:
				throw new UnsuitableGranularityException(timeInterval.timeUnit,
						api);
			}
			break;
		case MONTH:
			switch (timeInterval.timeUnit) {
			case DAY:
			case WEEK:
			case MONTH:
				start = getBeginningOfMonth(start);
				end = getEndOfMonth(end);
				break;
			case YEAR:
				throw new UnsuitableGranularityException(timeInterval.timeUnit,
						api);
			}
			break;
		case YEAR:
			start = getBeginningOfYear(start);
			end = getEndOfYear(end);
			break;
		}
		TimeInterval greedyInterval = new TimeInterval(start, end,
				timeInterval.timeUnit, UTC);
		return greedyInterval;
	}

	long getBeginningOfMonth(long start) {
		Calendar c = Calendar.getInstance(UTC);
		c.setTimeInMillis(start);
		int month = c.get(Calendar.MONTH);
		Calendar mc = Calendar.getInstance(UTC);
		mc.set(Calendar.MONTH, month);
		mc.set(Calendar.DAY_OF_MONTH, 1);
		long t = mc.getTimeInMillis();
		t = TimeUtils.fromMidnight(t, UTC);
		return t;
	}

	long getEndOfMonth(long end) {
		Calendar c = Calendar.getInstance(UTC);
		c.setTimeInMillis(end);
		int month = c.get(Calendar.MONTH);
		int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		Calendar mc = Calendar.getInstance(UTC);
		mc.set(Calendar.MONTH, month);
		mc.set(Calendar.DAY_OF_MONTH, daysInMonth);
		long t = mc.getTimeInMillis();
		t = TimeUtils.toMidnight(t, 0, UTC);
		return t;
	}

	long getEndOfWeek(long end) {
		Calendar c = Calendar.getInstance(UTC);
		c.setTimeInMillis(end);
		int week = c.get(Calendar.WEEK_OF_YEAR);
		Calendar wc = Calendar.getInstance(UTC);
		wc.setFirstDayOfWeek(Calendar.MONDAY);
		wc.set(Calendar.WEEK_OF_YEAR, week);
		wc.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		long t = wc.getTimeInMillis();
		t = TimeUtils.toMidnight(t, 0, UTC);
		return t;
	}

	long getBeginningOfWeek(long start) {
		Calendar c = Calendar.getInstance(UTC);
		c.setTimeInMillis(start);
		int week = c.get(Calendar.WEEK_OF_YEAR);
		Calendar wc = Calendar.getInstance(UTC);
		wc.setFirstDayOfWeek(Calendar.MONDAY);
		wc.set(Calendar.WEEK_OF_YEAR, week);
		wc.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		long t = wc.getTimeInMillis();
		t = TimeUtils.fromMidnight(t, UTC);
		return t;
	}

	long getBeginningOfYear(long start) {
		Calendar c = Calendar.getInstance(UTC);
		c.setTimeInMillis(start);
		int year = c.get(Calendar.YEAR);
		Calendar mc = Calendar.getInstance(UTC);
		mc.set(Calendar.YEAR, year);
		mc.set(Calendar.DAY_OF_YEAR, 1);
		long t = mc.getTimeInMillis();
		t = TimeUtils.fromMidnight(t, UTC);
		return t;
	}

	long getEndOfYear(long end) {
		Calendar c = Calendar.getInstance(UTC);
		c.setTimeInMillis(end);
		int year = c.get(Calendar.YEAR);
		int daysInYear = c.getActualMaximum(Calendar.DAY_OF_YEAR);
		Calendar mc = Calendar.getInstance(UTC);
		mc.set(Calendar.YEAR, year);
		mc.set(Calendar.DAY_OF_YEAR, daysInYear);
		long t = mc.getTimeInMillis();
		t = TimeUtils.toMidnight(t, 0, UTC);
		return t;
	}

}
