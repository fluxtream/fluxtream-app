package com.fluxtream.connectors.fitbit;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.Connector.UpdateStrategyType;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.connectors.updaters.UpdateInfo.UpdateType;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.metadata.DayMetadataFacet;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.TimeUtils;
import com.fluxtream.utils.Utils;

/**
 * @author candide
 * 
 */
@Component
@Controller
@Updater(prettyName = "Fitbit", value = 7, updateStrategyType = UpdateStrategyType.PUSH, objectTypes = {
		FitbitActivityFacet.class, FitbitLoggedActivityFacet.class,
		FitbitSleepFacet.class })
@JsonFacetCollection(FitbitFacetVOCollection.class)
public class FitBitTSUpdater extends AbstractUpdater {

	Logger logger = Logger.getLogger(FitBitTSUpdater.class);

	@Autowired
	SignpostOAuthHelper signpostHelper;

	@Autowired
	MetadataService metadataService;

	@Autowired
	ApiDataService apiDataService;

	private static final DateTimeFormatter dateFormat = DateTimeFormat
			.forPattern("yyyy-MM-dd");

	public final static long TWENTYFOUR_HOURS = 24 * 3600000;

	public static final String GET_STEPS_CALL = "FITBIT_GET_STEPS_TIMESERIES_CALL";
	public static final String GET_USER_PROFILE_CALL = "FITBIT_GET_USER_PROFILE_CALL";

	static {
		ObjectType.registerCustomObjectType(GET_STEPS_CALL);
		ObjectType.registerCustomObjectType(GET_USER_PROFILE_CALL);
	}

	public FitBitTSUpdater() {
		super();
	}

	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		ObjectType sleepOT = ObjectType.getObjectType(connector(), "sleep");
		ObjectType activityOT = ObjectType.getObjectType(connector(),
				"activity_summary");
		if (updateInfo.objectTypes().contains(sleepOT)) {
			loadTimeSeries("sleep/timeInBed", updateInfo.apiKey, sleepOT,
					"timeInBed");
			loadTimeSeries("sleep/startTime", updateInfo.apiKey, sleepOT,
					"startTime");
			loadTimeSeries("sleep/minutesAsleep", updateInfo.apiKey, sleepOT,
					"minutesAsleep");
			loadTimeSeries("sleep/minutesAwake", updateInfo.apiKey, sleepOT,
					"minutesAwake");
			loadTimeSeries("sleep/minutesToFallAsleep", updateInfo.apiKey,
					sleepOT, "minutesToFallAsleep");
			loadTimeSeries("sleep/minutesAfterWakeup", updateInfo.apiKey,
					sleepOT, "minutesAfterWakeup");
			loadTimeSeries("sleep/awakeningsCount", updateInfo.apiKey, sleepOT,
					"awakeningsCount");
		} else if (updateInfo.objectTypes().contains(activityOT)) {
			loadTimeSeries("activities/log/calories", updateInfo.apiKey,
					activityOT, "caloriesOut");
			loadTimeSeries("activities/log/steps", updateInfo.apiKey,
					activityOT, "steps");
			loadTimeSeries("activities/log/distance", updateInfo.apiKey,
					activityOT, "totalDistance");
			// loadTimeSeries("activities/log/elevation", updateInfo.apiKey,
			// activityOT,
			// "elevation");
			loadTimeSeries("activities/log/minutesSedentary",
					updateInfo.apiKey, activityOT, "sedentaryMinutes");
			loadTimeSeries("activities/log/minutesLightlyActive",
					updateInfo.apiKey, activityOT, "lightlyActiveMinutes");
			loadTimeSeries("activities/log/minutesFairlyActive",
					updateInfo.apiKey, activityOT, "fairlyActiveMinutes");
			loadTimeSeries("activities/log/minutesVeryActive",
					updateInfo.apiKey, activityOT, "veryActiveMinutes");
			loadTimeSeries("activities/log/activeScore", updateInfo.apiKey,
					activityOT, "activeScore");
			loadTimeSeries("activities/log/activityCalories",
					updateInfo.apiKey, activityOT, "activityCalories");
		}
	}

	public void updateCaloriesIntraday(FitbitActivityFacet facet, ApiKey apiKey)
			throws RateLimitReachedException {
		if (facet.date != null) {
			if (facet.caloriesJson == null
					|| isToday(facet.date, apiKey.getGuestId())) {
				String json = signpostHelper.makeRestCall(connector(), apiKey,
						"activities/log/calories/date".hashCode(),
						"http://api.fitbit.com/1/user/-/activities/log/calories/date/"
								+ facet.date + "/1d.json");
				facet.caloriesJson = json;
				facetDao.merge(facet);
			}
		} else {
			logger.warn("guestId=" + apiKey.getGuestId() +
					" connector=fitbit action=updateCaloriesIntraday message=facet date is null");
		}
	}

	private boolean isToday(String date, long guestId) {
		TimeZone tz = metadataService.getCurrentTimeZone(guestId);
		String today = dateFormat.withZone(DateTimeZone.forTimeZone(tz)).print(
				System.currentTimeMillis());
		return date.equals(today);
	}

	public void updateStepsIntraday(FitbitActivityFacet facet, ApiKey apiKey)
			throws RateLimitReachedException {
		if (facet.date != null) {
			String json = signpostHelper.makeRestCall(connector(), apiKey,
					"activities/log/steps/date".hashCode(),
					"http://api.fitbit.com/1/user/-/activities/log/steps/date/"
							+ facet.date + "/1d.json");
			facet.stepsJson = json;
			facetDao.merge(facet);
		} else {
			logger.warn("guestId=" + apiKey.getGuestId() +
					" connector=fitbit action=updateStepsIntraday message=facet date is null");
		}
	}

	public void loadTimeSeries(String uri, ApiKey apiKey,
			ObjectType objectType, String fieldName)
			throws RateLimitReachedException {
		String json = signpostHelper.makeRestCall(connector(), apiKey,
				uri.hashCode(), "http://api.fitbit.com/1/user/-/" + uri
						+ "/date/today/max.json");

		ObjectType sleepOT = ObjectType.getObjectType(connector(), "sleep");
		ObjectType activityOT = ObjectType.getObjectType(connector(),
				"activity_summary");

		JSONObject timeSeriesJson = JSONObject.fromObject(json);
		String resourceName = uri.replace('/', '-');
		JSONArray timeSeriesArray = timeSeriesJson.getJSONArray(resourceName);
		for (int i = 0; i < timeSeriesArray.size(); i++) {
			try {
				JSONObject entry = timeSeriesArray.getJSONObject(i);
				String date = entry.getString("dateTime");
				DayMetadataFacet dayMetadata = metadataService.getDayMetadata(
						apiKey.getGuestId(), date, true);

				TimeInterval timeInterval = dayMetadata.getTimeInterval();

				if (objectType == sleepOT) {
					FitbitSleepFacet facet = getSleepFacet(apiKey.getGuestId(),
							date);
					if (facet == null) {
						facet = new FitbitSleepFacet();
						facet.date = date;
						facet.api = connector().value();
						facet.guestId = apiKey.getGuestId();
						facetDao.persist(facet);
					}
					addToSleepFacet(facet, entry, fieldName, dayMetadata);
				} else if (objectType == activityOT) {
					FitbitActivityFacet facet = getActivityFacet(
							apiKey.getGuestId(), timeInterval);
					if (facet == null) {
						facet = new FitbitActivityFacet();
						facet.date = date;
						facet.api = connector().value();
						facet.guestId = apiKey.getGuestId();
						facet.start = dayMetadata.start;
						facet.end = dayMetadata.end;
						facetDao.persist(facet);
					}
					addToActivityFacet(facet, entry, fieldName);
				}

			} catch (Throwable t) {
				t.printStackTrace();
				continue;
			}
		}
	}

	private FitbitActivityFacet getActivityFacet(long guestId,
			TimeInterval timeInterval) {
		return jpaDaoService.findOne("fitbit.activity_summary.byStartEnd",
				FitbitActivityFacet.class, guestId, timeInterval.start,
				timeInterval.end);
	}

	private FitbitSleepFacet getSleepFacet(long guestId, String date) {
		return jpaDaoService.findOne("fitbit.sleep.byDate",
				FitbitSleepFacet.class, guestId, date);
	}

	@Transactional(readOnly = false)
	private void addToSleepFacet(FitbitSleepFacet facet, JSONObject entry,
			String fieldName, DayMetadataFacet md) {
		if (fieldName.equals("startTime")) {
			storeTime(entry.getString("value"), facet, md);
		} else
			setFieldValue(facet, fieldName, entry.getString("value"));
		facetDao.merge(facet);
	}

	private final static DateTimeFormatter format = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

	private void storeTime(String bedTimeString, FitbitSleepFacet facet,
			DayMetadataFacet md) {
		if (bedTimeString.equals("")) // bedTimeString EST TOUJOURS EGAL A ""!!!
			return;
		if (bedTimeString.length() == 5)
			bedTimeString = facet.date + "T" + bedTimeString + ":00.000";
		facet.startTimeStorage = bedTimeString;
		// using UTC just to have a reference point in order to
		// compute riseTime with a duration delta from bedTime
		MutableDateTime bedTimeUTC = null;
		bedTimeUTC = format.withZone(DateTimeZone.forID("UTC"))
				.parseMutableDateTime(bedTimeString);
		bedTimeUTC.add(facet.timeInBed * 60000);
		String riseTimeString = format.withZone(DateTimeZone.forID("UTC"))
				.print(bedTimeUTC.getMillis());
		facet.start = format.withZone(DateTimeZone.forID(md.timeZone))
				.parseDateTime(bedTimeString).getMillis();
		facet.end = format.withZone(DateTimeZone.forID(md.timeZone))
				.parseDateTime(riseTimeString).getMillis();
		facet.endTimeStorage = riseTimeString;
	}

	@Transactional(readOnly = false)
	private void addToActivityFacet(FitbitActivityFacet facet,
			JSONObject entry, String fieldName) {
		setFieldValue(facet, fieldName, entry.getString("value"));
		facetDao.merge(facet);
	}

	private void setFieldValue(Object o, String fieldName, String stringValue) {
		try {
			Field field = o.getClass().getField(fieldName);
			Class<?> type = field.getType();
			Object value = null;
			if (type == String.class)
				value = stringValue;
			else if (type == Integer.TYPE)
				value = Integer.valueOf(stringValue);
			else if (type == Double.TYPE)
				value = Double.valueOf(stringValue);
			else if (type == Float.TYPE)
				value = Float.valueOf(stringValue);
			field.set(o, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		JSONObject jsonParams = JSONObject.fromObject(updateInfo.jsonParams);
		FitbitUserProfile userProfile = guestService.getUserProfile(
				updateInfo.apiKey.getGuestId(), FitbitUserProfile.class);
		TimeZone userTimeZone = userProfile.getTimeZone();
		String dateString = jsonParams.getString("date");
		Date date = new Date(dateFormat.withZone(
				DateTimeZone.forTimeZone(userTimeZone)).parseMillis(dateString));

		ObjectType sleepObjectType = ObjectType.getObjectType(connector(),
				"sleep");
		ObjectType activityObjectType = ObjectType.getObjectType(connector(),
				"activity_summary");
		ObjectType loggedActivityObjectType = ObjectType.getObjectType(
				connector(), "logged_activity");

		long from = TimeUtils.fromMidnight(date.getTime(), userTimeZone);
		long to = TimeUtils.toMidnight(date.getTime(), userTimeZone);
		TimeInterval timeInterval = new TimeInterval(from, to, TimeUnit.DAY,
				userTimeZone);

		if (updateInfo.objectTypes().contains(sleepObjectType)) {
			apiDataService.eraseApiData(updateInfo.getGuestId(),
					updateInfo.apiKey.getConnector(), sleepObjectType,
					timeInterval);
			try {
				loadSleepDataForOneDay(updateInfo, date, userTimeZone);
			} catch (RuntimeException e) {
				logger.warn("guestId=" + updateInfo.getGuestId() +
						" connector=fitbit objectType=activity action=historyUpdate exception="
								+ Utils.mediumStackTrace(e));
			}
		}
		if (updateInfo.objectTypes().contains(activityObjectType)) {
			apiDataService.eraseApiData(updateInfo.getGuestId(),
					updateInfo.apiKey.getConnector(), activityObjectType,
					timeInterval);
			apiDataService.eraseApiData(updateInfo.getGuestId(),
					updateInfo.apiKey.getConnector(), loggedActivityObjectType,
					timeInterval);
			try {
				loadActivityDataForOneDay(updateInfo, date, userTimeZone);
			} catch (RuntimeException e) {
				logger.warn("guestId=" + updateInfo.getGuestId() +
						" connector=fitbit objectType=activity action=historyUpdate exception="
								+ Utils.shortStackTrace(e));
			}
		}
	}

	private void loadActivityDataForOneDay(UpdateInfo updateInfo, Date date,
			TimeZone timeZone) throws RateLimitReachedException, Exception {
		String json = getActivityData(updateInfo, date, timeZone);
		long fromMidnight = TimeUtils.fromMidnight(date.getTime(), timeZone);
		long toMidnight = TimeUtils.toMidnight(date.getTime(), timeZone);
		logger.info("guestId=" + updateInfo.getGuestId() +
				" connector=fitbit action=loadActivityDataForOneDay json="
						+ json);
		if (json != null) {
			apiDataService.cacheApiDataJSON(updateInfo, json, fromMidnight,
					toMidnight);
		} else
			apiDataService.cacheEmptyData(updateInfo, fromMidnight, toMidnight);
	}

	private void loadSleepDataForOneDay(UpdateInfo updateInfo, Date date,
			TimeZone timeZone) throws RateLimitReachedException, Exception {
		String json = getSleepData(updateInfo, date, timeZone);
		long fromMidnight = TimeUtils.fromMidnight(date.getTime(), timeZone);
		long toMidnight = TimeUtils.toMidnight(date.getTime(), timeZone);
		if (json != null) {
			apiDataService.cacheApiDataJSON(updateInfo, json, fromMidnight,
					toMidnight);
		} else
			apiDataService.cacheEmptyData(updateInfo, fromMidnight, toMidnight);
	}

	private String getSleepData(UpdateInfo updateInfo, Date date,
			TimeZone timeZone) throws RateLimitReachedException {
		// we want the date formatted as where the user was that day
		String formattedDate = dateFormat.withZone(
				DateTimeZone.forTimeZone(timeZone)).print(date.getTime());

		String urlString = "http://api.fitbit.com/1/user/-/sleep/date/"
				+ formattedDate + ".json";

		String json = signpostHelper.makeRestCall(connector(),
				updateInfo.apiKey, updateInfo.objectTypes, urlString);

		return json;
	}

	private String getActivityData(UpdateInfo updateInfo, Date date,
			TimeZone timeZone) throws RateLimitReachedException {
		// we want the date formatted as where the user was that day
		String formattedDate = dateFormat.withZone(
				DateTimeZone.forTimeZone(timeZone)).print(date.getTime());

		String urlString = "http://api.fitbit.com/1/user/-/activities/date/"
				+ formattedDate + ".json";

		String json = signpostHelper.makeRestCall(connector(),
				updateInfo.apiKey, updateInfo.objectTypes, urlString);

		return json;
	}

	@RequestMapping("/fitbit/notify")
	public void notifyMeasurement(@RequestBody String updatesString,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		String lines[] = updatesString.split("\\r?\\n");

		for (String line : lines) {
			if (line.startsWith("[{\"collectionType")) {
				updatesString = line;
				break;
			}
		}

		logger.info("action=apiNotification connector=fitbit message="
				+ updatesString);

		try {
			JSONArray updatesArray = JSONArray.fromObject(updatesString);
			for (int i = 0; i < updatesArray.size(); i++) {
				JSONObject jsonUpdate = updatesArray.getJSONObject(i);
				String collectionType = jsonUpdate.getString("collectionType");
				// warning: 'body' doesn't have a date!!!
				if (collectionType.equals("body"))
					continue;
				String dateString = jsonUpdate.getString("date");
				String ownerId = jsonUpdate.getString("ownerId");
				String subscriptionId = jsonUpdate.getString("subscriptionId");

				int objectTypes = 0;
				if (collectionType.equals("foods")
						|| collectionType.equals("body")) {
					continue;
				} else if (collectionType.equals("activities")) {
					objectTypes = 3;
				} else if (collectionType.equals("sleep")) {
					objectTypes = 4;
				}

				FitbitUserProfile userProfile = jpaDaoService.findOne(
						"fitbitUser.byEncodedId", FitbitUserProfile.class,
						ownerId);

				connectorUpdateService.addApiNotification(connector(),
						userProfile.guestId, updatesString);

				JSONObject jsonParams = new JSONObject();
				jsonParams.accumulate("date", dateString)
						.accumulate("ownerId", ownerId)
						.accumulate("subscriptionId", subscriptionId);

				logger.info("action=scheduleUpdate connector=fitbit collectionType="
						+ collectionType);

				connectorUpdateService.scheduleUpdate(userProfile.guestId,
						connector().getName(), objectTypes,
						UpdateType.PUSH_TRIGGERED_UPDATE,
						System.currentTimeMillis() + 5000,
						jsonParams.toString());
			}
		} catch (Exception e) {
			System.out.println("error processing fitbit notification " + updatesString);
			e.printStackTrace();
			logger.warn("Could not parse fitbit notification: "
					+ Utils.stackTrace(e));
		}
	}

}
