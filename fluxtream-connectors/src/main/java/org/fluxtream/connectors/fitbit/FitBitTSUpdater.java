package org.fluxtream.connectors.fitbit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Autonomous;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.SignpostOAuthHelper;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.RateLimitReachedException;
import org.fluxtream.core.connectors.updaters.UnexpectedResponseCodeException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.fluxtream.core.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author candide
 * 
 */
@Component
@Controller
@Updater(prettyName = "Fitbit", value = 7, objectTypes = {
		FitbitTrackerActivityFacet.class, FitbitLoggedActivityFacet.class,
		FitbitSleepFacet.class, FitbitWeightFacet.class },
        bodytrackResponder = FitbitBodytrackResponder.class,
        defaultChannels = {"Fitbit.steps","Fitbit.caloriesOut"})
public class FitBitTSUpdater extends AbstractUpdater implements Autonomous {

	FlxLogger logger = FlxLogger.getLogger(FitBitTSUpdater.class);

	@Autowired
	SignpostOAuthHelper signpostHelper;

	@Autowired
	MetadataService metadataService;

	@Autowired
	ApiDataService apiDataService;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

	public static final String GET_STEPS_CALL = "FITBIT_GET_STEPS_TIMESERIES_CALL";
	public static final String GET_USER_PROFILE_CALL = "FITBIT_GET_USER_PROFILE_CALL";
    public static final String GET_USER_DEVICES_CALL = "FITBIT_GET_USER_DEVICES_CALL";

    final ObjectType sleepOT = ObjectType.getObjectType(connector(),
                                                          "sleep");
    final ObjectType weightOT = ObjectType.getObjectType(connector(),
                                                        "weight");
    final ObjectType activityOT = ObjectType.getObjectType(connector(),
                                                             "activity_summary");
    final ObjectType loggedActivityOT = ObjectType.getObjectType(
            connector(), "logged_activity");

    static {
		ObjectType.registerCustomObjectType(GET_STEPS_CALL);
		ObjectType.registerCustomObjectType(GET_USER_PROFILE_CALL);
        ObjectType.registerCustomObjectType(GET_USER_DEVICES_CALL);
	}

	public FitBitTSUpdater() {
		super();
	}

    private void initChannelMapping(UpdateInfo updateInfo) {
        //TODO: figure out how to support date-based facets in the timeline
        //List<ChannelMapping> mappings = bodyTrackHelper.getChannelMappings(updateInfo.apiKey);
        //if (mappings.size() == 0){
        //    ChannelMapping mapping = new ChannelMapping();
        //    mapping.deviceName = "fitbit";
        //    mapping.channelName = "sleep";
        //    mapping.timeType = ChannelMapping.TimeType.gmt;
        //    mapping.channelType = ChannelMapping.ChannelType.timespan;
        //    mapping.guestId = updateInfo.getGuestId();
        //    mapping.apiKeyId = updateInfo.apiKey.getId();
        //    mapping.objectTypeId = ObjectType.getObjectType(updateInfo.apiKey.getConnector(), "sleep").value();
        //    bodyTrackHelper.persistChannelMapping(mapping);
        //
        //    BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        //    channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        //    channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        //    channelStyle.timespanStyles.defaultStyle.fillColor = "#21b5cf";
        //    channelStyle.timespanStyles.defaultStyle.borderColor = "#21b5cf";
        //    channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
        //    channelStyle.timespanStyles.defaultStyle.top = 0.0;
        //    channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
        //    channelStyle.timespanStyles.values = new HashMap();
        //
        //    BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
        //    stylePart.top = 0.25;
        //    stylePart.bottom = 0.75;
        //    stylePart.fillColor = "#21b5cf";
        //    stylePart.borderColor = "#21b5cf";
        //    channelStyle.timespanStyles.values.put("on",stylePart);
        //
        //    bodyTrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(),"fitbit","sleep",channelStyle);
        //}
    }

	@Override
	public void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception, UnexpectedResponseCodeException {
        try {
            // sleep

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
            loadTimeSeries("sleep/awakeningsCount", updateInfo.apiKey, sleepOT, "awakeningsCount");

            // activities

            loadTimeSeries("activities/tracker/calories", updateInfo.apiKey,
                           activityOT, "caloriesOut");
            loadTimeSeries("activities/tracker/steps", updateInfo.apiKey,
                           activityOT, "steps");
            loadTimeSeries("activities/tracker/distance", updateInfo.apiKey,
                           activityOT, "totalDistance");

            // The floors and elevation APIs report 400 errors if called on
            // an account which has never been bound to a Fitbit device which
            // has an altimeter, such as the Fitbit Ultra.  For now, disable
            // reading these APIs.  In the future, perhaps check the device
            // type and conditionally call these APIs.
            loadTimeSeries("activities/tracker/floors", updateInfo.apiKey,
                           activityOT, "floors");
            loadTimeSeries("activities/tracker/elevation", updateInfo.apiKey,
                           activityOT, "elevation");
            loadTimeSeries("activities/tracker/minutesSedentary",
                           updateInfo.apiKey, activityOT, "sedentaryMinutes");
            loadTimeSeries("activities/tracker/minutesLightlyActive",
                           updateInfo.apiKey, activityOT, "lightlyActiveMinutes");
            loadTimeSeries("activities/tracker/minutesFairlyActive",
                           updateInfo.apiKey, activityOT, "fairlyActiveMinutes");
            loadTimeSeries("activities/tracker/minutesVeryActive",
                           updateInfo.apiKey, activityOT, "veryActiveMinutes");
            loadTimeSeries("activities/tracker/activeScore", updateInfo.apiKey,
                           activityOT, "activeScore");
            loadTimeSeries("activities/tracker/activityCalories",
                           updateInfo.apiKey, activityOT, "activityCalories");

            // weight
            // Store the time when we're asking about the weight in case this
            // account doesn't have a hardware scale associated with it
            long weightRequestMillis = System.currentTimeMillis();

            loadTimeSeries("body/weight", updateInfo.apiKey, weightOT,
                           "weight");
            loadTimeSeries("body/bmi", updateInfo.apiKey, weightOT,
                           "bmi");
            loadTimeSeries("body/fat", updateInfo.apiKey, weightOT,
                           "fat");

            jpaDaoService.execute("DELETE FROM Facet_FitbitSleep sleep WHERE sleep.start=0");
            final JSONArray deviceStatusesArray = getDeviceStatusesArray(updateInfo.apiKey);

            // Store TRACKER.lastSyncDate
            long trackerLastSyncDate = -1;

            try {
                trackerLastSyncDate = getLastServerSyncMillis(deviceStatusesArray, "TRACKER");
            }
            catch (Throwable t) {
                logger.info("guestId=" + updateInfo.getGuestId() +
                            " connector=fitbit action=updateConnectorDataHistory " +
                            " message=\"Error getting TRACKER.lastSyncDate\" stackTrace=<![CDATA[\"" + t.getStackTrace() + "]]>");
            }

            if(trackerLastSyncDate==-1) {
                // Default to yesterday if no better value is available
                trackerLastSyncDate = System.currentTimeMillis() - DateTimeConstants.MILLIS_PER_DAY;
            }
            guestService.setApiKeyAttribute(updateInfo.apiKey, "TRACKER.lastSyncDate",
                                            String.valueOf(trackerLastSyncDate));

            // Store SCALE.lastSyncDate
            long scaleLastSyncDate = -1;

            try {
                scaleLastSyncDate = getLastServerSyncMillis(deviceStatusesArray, "SCALE");
            } catch (Throwable t) {
                logger.info("guestId=" + updateInfo.getGuestId() +
                            " connector=fitbit action=updateConnectorDataHistory " +
                            " message=\"Error getting SCALE.lastSyncDate\" stackTrace=<![CDATA[\"" + t.getStackTrace() + "]]>");
            }

            // In the case that the scale doesn't have a valid scaleLastSyncDate, store
            // the timestamp for when we asked about the weight for use in doing incremental
            // weight updates later on
            if(scaleLastSyncDate == -1) {
                guestService.setApiKeyAttribute(updateInfo.apiKey, "SCALE.lastSyncDate",
                                                            String.valueOf(weightRequestMillis));
            }
            else {
                guestService.setApiKeyAttribute(updateInfo.apiKey, "SCALE.lastSyncDate",
                                            String.valueOf(scaleLastSyncDate));
            }

            // Flush the initial fitbit history data to the datastore.
            // This is handled automatically by the incremental updates because
            // it uses the apiDataService.cacheApiDataJSON APIs.  However,
            // the above code does not do that so we explicity send the
            // Fitbit facet data to the datastore here.
            bodyTrackStorageService.storeInitialHistory(updateInfo.apiKey);
            initChannelMapping(updateInfo);
        } catch (UnexpectedResponseCodeException e) {
            // Check for response code 409 which is Fitbit's over rate limit error
            if(e.responseCode == 409) {
                throw new RateLimitReachedException(e);
            }
            else{
                // Otherwise throw the same error that SignpostOAuthHelper used to throw
                throw new RuntimeException(
                        "Could not make REST call, got response code: "
                        + e.responseCode + ", message: "
                        + e.getMessage() + "\n+REST url: "
                        + e.URL);
            }
        }
    }

    public long getLastWeighingTime(final UpdateInfo updateInfo) {
        final FitbitWeightFacet weightFacet = jpaDaoService.findOne("fitbit.weight.latest", FitbitWeightFacet.class, updateInfo.apiKey.getId());
        if(weightFacet!=null) {
            return weightFacet.start;
        }
        else {
            return -1;
        }
    }

    public void updateCaloriesIntraday(FitbitTrackerActivityFacet facet, ApiKey apiKey)
			throws RateLimitReachedException, UnexpectedResponseCodeException {
		if (facet.date != null) {
			if (facet.caloriesJson == null
					|| isToday(facet.date, apiKey.getGuestId())) {
				String json = signpostHelper.makeRestCall(apiKey,
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
		String today = TimeUtils.dateFormatter.withZone(DateTimeZone.forTimeZone(tz)).print(
				System.currentTimeMillis());
		return date.equals(today);
	}

	public void updateStepsIntraday(FitbitTrackerActivityFacet facet, ApiKey apiKey)
			throws RateLimitReachedException, UnexpectedResponseCodeException {
		if (facet.date != null) {
			String json = signpostHelper.makeRestCall(apiKey,
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
			throws RateLimitReachedException, UnexpectedResponseCodeException {

        String json = "";
        try {
            json = signpostHelper.makeRestCall(apiKey,
                    uri.hashCode(), "http://api.fitbit.com/1/user/-/" + uri
                            + "/date/today/max.json");
        } catch (Throwable t) {
            // elevation and floors are not available for earlier trackers, so we can safely ignore them
            if (fieldName.equals("elevation")||fieldName.equals("floors")) {
                logger.info("guestId=" + apiKey.getGuestId() +
                            " connector=fitbit action=loadTimeSeries message=\"Could not load timeseries for " + fieldName);
                return;
            }
        }

		JSONObject timeSeriesJson = JSONObject.fromObject(json);
		String resourceName = uri.replace('/', '-');
		JSONArray timeSeriesArray = timeSeriesJson.getJSONArray(resourceName);
		for (int i = 0; i < timeSeriesArray.size(); i++) {
            JSONObject entry = timeSeriesArray.getJSONObject(i);
            String date = entry.getString("dateTime");


            if (objectType == sleepOT) {
                FitbitSleepFacet facet = getSleepFacet(apiKey.getId(),
                                                       date);
                if (facet == null) {
                    facet = new FitbitSleepFacet(apiKey.getId());
                    facet.date = date;
                    facet.api = connector().value();
                    facet.guestId = apiKey.getGuestId();
                    facetDao.persist(facet);
                }
                addToSleepFacet(facet, entry, fieldName);
            } else if (objectType == activityOT) {
                FitbitTrackerActivityFacet facet = getActivityFacet(
                        apiKey.getId(), date);
                if (facet == null) {
                    facet = new FitbitTrackerActivityFacet(apiKey.getId());
                    facet.date = date;
                    facet.api = connector().value();
                    final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(date);

                    facet.start = dateTime.getMillis();
                    facet.end = dateTime.getMillis()+ DateTimeConstants.MILLIS_PER_DAY-1;

                    facet.startTimeStorage = date + "T00:00:00.000";
                    facet.endTimeStorage = date + "T23:59:59.999";

                    facet.guestId = apiKey.getGuestId();
                    facetDao.persist(facet);
                }
                addToActivityFacet(facet, entry, fieldName);
            } else if (objectType == weightOT) {
                FitbitWeightFacet facet = getWeightFacet(apiKey.getId(), date);
                if (facet == null) {
                    facet = new FitbitWeightFacet(apiKey.getId());
                    facet.date = date;
                    facet.api = connector().value();
                    facet.guestId = apiKey.getGuestId();

                    final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(date);

                    facet.start = dateTime.getMillis();
                    facet.end = dateTime.getMillis() + DateTimeConstants.MILLIS_PER_DAY - 1;

                    facet.startTimeStorage = date + "T00:00:00.000";
                    facet.endTimeStorage = date + "T23:59:59.999";

                    facetDao.persist(facet);
                }
                addToWeightFacet(facet, entry, fieldName);
            }
		}
	}

    @Transactional(readOnly = false)
    private void addToWeightFacet(FitbitWeightFacet facet, JSONObject entry, String fieldName) {
        setFieldValue(facet, fieldName, entry.getString("value"));
        facetDao.merge(facet);
    }

    private FitbitWeightFacet getWeightFacet(final long apiKeyId, String date) {
        return jpaDaoService.findOne("fitbit.weight.byDate",
                                     FitbitWeightFacet.class, apiKeyId, date);
    }

    private FitbitTrackerActivityFacet getActivityFacet(long apiKeyId,
			String date) {
		return jpaDaoService.findOne("fitbit.activity_summary.byDate",
				FitbitTrackerActivityFacet.class, apiKeyId, date);
	}

	private FitbitSleepFacet getSleepFacet(long apiKeyId, String date) {
		return jpaDaoService.findOne("fitbit.sleep.byDate",
				FitbitSleepFacet.class, apiKeyId, date);
	}

	@Transactional(readOnly = false)
	private void addToSleepFacet(FitbitSleepFacet facet, JSONObject entry,
			String fieldName) {
		if (fieldName.equals("startTime")) {
			storeTime(entry.getString("value"), facet);
		} else
			setFieldValue(facet, fieldName, entry.getString("value"));
		facetDao.merge(facet);
	}

	private final static DateTimeFormatter format = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

	private void storeTime(String bedTimeString, FitbitSleepFacet facet) {
		if (bedTimeString.equals("")) // bedTimeString EST TOUJOURS EGAL A ""!!!
			return;
		if (bedTimeString.length() == 5)
			bedTimeString = facet.date + "T" + bedTimeString + ":00.000";
		// using UTC just to have a reference point in order to
		// compute riseTime with a duration delta from bedTime
		facet.start = format.withZoneUTC().parseMillis(bedTimeString);
        facet.end = facet.start + facet.timeInBed * 60000;
	}

	@Transactional(readOnly = false)
	private void addToActivityFacet(FitbitTrackerActivityFacet facet,
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

    public List<String> getDaysToSync(UpdateInfo updateInfo, final String deviceType, long trackerLastServerSyncMillis, long scaleLastServerSyncMillis)
            throws RateLimitReachedException
    {
        ApiKey apiKey = updateInfo.apiKey;
        long lastStoredSyncMillis = 0;
        if (deviceType.equals("TRACKER")) {
            // TRACKER.lastSyncDate is actually used to record our progress in where we got to at the end of the last 
            // successful incremental update.  It should be <= the actual "lastSyncTime" returned by the server
            // during the last update
            try {
                final String trackerLastStoredSyncMillis = guestService.getApiKeyAttribute(apiKey, "TRACKER.lastSyncDate");
                lastStoredSyncMillis = Long.valueOf(trackerLastStoredSyncMillis);
            } catch (Throwable t) {
                // As a fallback, get the latest facet in the DB and use the start time from it as lastStoredSyncMillis
                final String entityName = JPAUtils.getEntityName(FitbitTrackerActivityFacet.class);
                final List<FitbitTrackerActivityFacet> newest = jpaDaoService.executeQueryWithLimit(
                        "SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC",
                        1,
                        FitbitTrackerActivityFacet.class, updateInfo.apiKey.getId());

                // If there are existing fitbit facets, use the start field of the most recent one.
                // If there are no existing fitbit facets, just start with today
                if (newest.size()>0) {
                    lastStoredSyncMillis = newest.get(0).start;
                    System.out.println("Fitbit: guestId=" + updateInfo.getGuestId() + ", using DB for lastStoredSyncMillis=" + lastStoredSyncMillis);
                }
                else {
                    System.out.println("Fitbit: guestId=" + updateInfo.getGuestId() + ", nothing in DB for lastStoredSyncMillis, default to yesterday");
                    lastStoredSyncMillis = System.currentTimeMillis()-DateTimeConstants.MILLIS_PER_DAY;
                }

                // Now make sure we're getting at least one day of data by setting lastStoredSyncMillis to
                // trackerLastServerSyncMillis if it's not already less
                if(trackerLastServerSyncMillis < lastStoredSyncMillis) {
                    lastStoredSyncMillis = trackerLastServerSyncMillis;
                }
                logger.info("guestId=" + updateInfo.getGuestId() +
                            " connector=fitbit action=getDaysToSync deviceType="+ deviceType +
                            " message=\"Error parsing TRACKER.lastSyncDate, using default of " + lastStoredSyncMillis +
                            "\" error=" + t.getMessage() + " stackTrace=<![CDATA[\"" + Utils.stackTrace(t) + "]]>");
            }
        } else {
            final String scaleLastSyncMillis = guestService.getApiKeyAttribute(apiKey, "SCALE.lastSyncDate");
            lastStoredSyncMillis = Long.valueOf(scaleLastSyncMillis);
        }
        if (deviceType.equals("TRACKER")&&lastStoredSyncMillis<= trackerLastServerSyncMillis) {
            // For the tracker, we only want to update if the device has really updated and we want to update
            // from the date corresponding to trackerLastStoredSyncMillis
            // to the date of scaleLastServerSyncMillis
            return getListOfDatesBetween(lastStoredSyncMillis, trackerLastServerSyncMillis);
        }
        else if (deviceType.equals("SCALE")) {
            // In the case of an account without a hardware scale, the server never returns a valid scale sync time,
            // so just use the dates between the day before the last time we did an update and now, ignoring scaleLastServerSyncMillis.
            // This means that we'll never get account-linked scale data that was delayed by more than a day, but that's the best
            // we can do for now.
            long endMillis = scaleLastServerSyncMillis;
            if(scaleLastServerSyncMillis==-1) {
                // No hardware scale.  Check if lastStoredSyncMillis is also -1, which will happen for
                // accounts last updated with a version prior to 0.9.0022 without a hardware scale.
                // In that case, start from the date of the last stored weight data in the facet DB.
                // This is a hack, but it's the best I can think of for now.
                if(lastStoredSyncMillis == -1) {
                    lastStoredSyncMillis = getLastWeighingTime(updateInfo);
                }
                if(lastStoredSyncMillis == -1) {
                    // If we don't have any weight data at all, just start with yesterday.
                    lastStoredSyncMillis = System.currentTimeMillis()-DateTimeConstants.MILLIS_PER_DAY;
                }
                // Use now as the end time, since we don't have better info from the server about when the
                // most recent data point might be
                endMillis = System.currentTimeMillis();
            }
            else if(scaleLastServerSyncMillis == lastStoredSyncMillis) {
                // We have a hardware scale and it hasn't updated since last time, don't need to sync at all
                return new ArrayList<String>();
            }

            return getListOfDatesBetween(lastStoredSyncMillis,
                                         endMillis);
        }
        return new ArrayList<String>();
    }

    private long getLastServerSyncMillis(JSONArray devices, String device) {
        try {
            for (int i=0; i<devices.size(); i++) {
                JSONObject deviceStatus = devices.getJSONObject(i);
                String type = deviceStatus.getString("type");
                String dateTime = deviceStatus.getString("lastSyncTime");
                long ts = AbstractLocalTimeFacet.timeStorageFormat.parseMillis(dateTime);
                if (type.equalsIgnoreCase(device)) {
                    return ts;
                }
            }
        } catch (Throwable t) {
            logger.info("connector=fitbit action=getLastServerSyncMillis message=\"Error parsing lastSyncTime from fitbit json\"  devices=\"" + devices.toString() +
                        "\" stackTrace=<![CDATA[\"" + Utils.stackTrace(t) + "]]>");
        }
        return -1;
    }

    private List<String> getListOfDatesBetween(final long startMillis, final long endMillis) {
        List<String> dates = new ArrayList<String>();
        // TODO: what really matters here is the date that the Fitbit server uses for the
        // tracker update times, so this isn't quite right
        final DateTimeZone utc = DateTimeZone.UTC;
        final LocalDate startDate = new LocalDate(startMillis, utc);
        // in order to have the date of endMillis in the resulting list we need to add a
        // a day
        final long dayAfterEnd = endMillis + DateTimeConstants.MILLIS_PER_DAY;
        int days = Days.daysBetween(startDate, new LocalDate(dayAfterEnd, utc)).getDays();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.withFieldAdded(DurationFieldType.days(), i);
            String dateString = TimeUtils.dateFormatter.print(d.toDateTimeAtStartOfDay().getMillis());
            dates.add(dateString);
        }
        return dates;
    }

    private JSONArray getDeviceStatusesArray(final ApiKey apiKey)
            throws RateLimitReachedException, UnexpectedResponseCodeException {
        String urlString = "http://api.fitbit.com/1/user/-/devices.json";

        final ObjectType customObjectType = ObjectType.getCustomObjectType(GET_USER_DEVICES_CALL);
        final int getUserDevicesObjectTypeID = customObjectType.value();
        String json = signpostHelper.makeRestCall(apiKey, getUserDevicesObjectTypeID,
                                                  urlString);
        return JSONArray.fromObject(json);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        try {
            final JSONArray deviceStatusesArray = getDeviceStatusesArray(updateInfo.apiKey);
            final long trackerLastServerSyncMillis = getLastServerSyncMillis(deviceStatusesArray, "TRACKER");
            final long scaleLastServerSyncMillis = getLastServerSyncMillis(deviceStatusesArray, "SCALE");

            if (trackerLastServerSyncMillis > -1) {
                final List<String> trackerDaysToSync = getDaysToSync(updateInfo, "TRACKER", trackerLastServerSyncMillis, scaleLastServerSyncMillis);
                if (trackerDaysToSync.size() > 0) {
                    updateTrackerListOfDays(updateInfo, trackerDaysToSync, trackerLastServerSyncMillis);
                    guestService.setApiKeyAttribute(updateInfo.apiKey, "TRACKER.lastSyncDate", String.valueOf(trackerLastServerSyncMillis));
                }
            }
            // Update the scale
            final List<String> scaleDaysToSync = getDaysToSync(updateInfo, "SCALE", trackerLastServerSyncMillis, scaleLastServerSyncMillis);
            if (scaleDaysToSync.size() > 0) {
                long weightRequestMillis = System.currentTimeMillis();
                updateScaleListOfDays(updateInfo, scaleDaysToSync, scaleLastServerSyncMillis);
                // Update SCALE.lastSyncDate: In the case that the scale doesn't have a valid scaleLastSyncDate, store
                // the timestamp for when we asked about the weight for use in doing incremental
                // weight updates later on
                if(scaleLastServerSyncMillis == -1) {
                    guestService.setApiKeyAttribute(updateInfo.apiKey, "SCALE.lastSyncDate",
                                                    String.valueOf(weightRequestMillis));
                }
                else {
                    guestService.setApiKeyAttribute(updateInfo.apiKey, "SCALE.lastSyncDate",
                                                    String.valueOf(scaleLastServerSyncMillis));
                }
            }
            initChannelMapping(updateInfo);
        } catch (UnexpectedResponseCodeException e) {
            // Check for response code 409 which is Fitbit's over rate limit error
            if(e.responseCode == 409) {
                throw new RateLimitReachedException(e);
            }
            else{
                // Otherwise throw the same error that SignpostOAuthHelper used to throw
                throw new RuntimeException(
                						"Could not make REST call, got response code: "
                								+ e.responseCode + ", message: "
                								+ e.getMessage() + "\n+REST url: "
                								+ e.URL);
               }
        }
	}

    private void updateTrackerListOfDays(final UpdateInfo updateInfo,
                                         final List<String> trackerDaysToSync, final long trackerLastServerSyncMillis) throws Exception {
        for (String dateString : trackerDaysToSync) {
            final TimeZone timeZone = TimeZone.getTimeZone("UTC");
            Date date = new Date(TimeUtils.dateFormatter.withZone(
                    DateTimeZone.forTimeZone(timeZone)).parseMillis(dateString));
            updateOneDayOfTrackerData(updateInfo, timeZone, date,
                                      dateString, trackerLastServerSyncMillis);
        }
    }

    private void updateScaleListOfDays(final UpdateInfo updateInfo,
                                             final List<String> scaleDaysToSync, final long scaleLastServerSyncMillis) throws Exception {
            for (String dateString : scaleDaysToSync) {
                final TimeZone timeZone = TimeZone.getTimeZone("UTC");
                Date date = new Date(TimeUtils.dateFormatter.withZone(
                        DateTimeZone.forTimeZone(timeZone)).parseMillis(dateString));
                updateOneDayOfScaleData(updateInfo, timeZone, date,
                                          dateString, scaleLastServerSyncMillis);
            }
        }

    private void updateOneDayOfTrackerData(final UpdateInfo updateInfo,
                                           final TimeZone userTimeZone, final Date date,
                                           final String dateString, final long trackerLastServerSyncMillis) throws Exception {
        updateInfo.setContext("date", dateString);

        logger.info("guestId=" + updateInfo.getGuestId() + " objectType=sleep" +
                    " connector=fitbit action=updateOneDayOfData date=" + dateString);
        apiDataService.eraseApiData(updateInfo.apiKey, sleepOT,
                                    Arrays.asList(dateString));
        loadSleepDataForOneDay(updateInfo, date, userTimeZone, dateString);

        logger.info("guestId=" + updateInfo.getGuestId() + " objectType=activity" +
                    " connector=fitbit action=updateOneDayOfData date=" + dateString);
        apiDataService.eraseApiData(updateInfo.apiKey, activityOT, Arrays.asList(dateString));
        apiDataService.eraseApiData(updateInfo.apiKey, loggedActivityOT,
                                    Arrays.asList(dateString));

        loadActivityDataForOneDay(updateInfo, date, userTimeZone, dateString);

        // If all that succeeded, record the minimum of the end of the day "date" and scaleLastServerSyncMillis
        // as TRACKER.lastSyncDate.  If scaleLastServerSyncMillis> the end of the day "date" then we already know
        // we have complete data for "date", and if we fail we should start with the day after that.
        // If scaleLastServerSyncMillis < the end of the day "date" then we have incomplete data for today
        // and will need to try it again next time.

        // Compute the start and end of that date in milliseconds for comparing
        // and truncating start/end
        DateTimeZone dateTimeZone = DateTimeZone.UTC;
        LocalDate localDate = LocalDate.parse(dateString);
        long dateEndMillis = localDate.toDateTimeAtStartOfDay(dateTimeZone).getMillis() + DateTimeConstants.MILLIS_PER_DAY;

        if(dateEndMillis<trackerLastServerSyncMillis) {
            guestService.setApiKeyAttribute(updateInfo.apiKey, "TRACKER.lastSyncDate",
                                            String.valueOf(dateEndMillis));
        }
    }

    private void updateOneDayOfScaleData(final UpdateInfo updateInfo,
                                           final TimeZone userTimeZone, final Date date,
                                           final String dateString, final long scaleLastServerSyncMillis) throws Exception {
        updateInfo.setContext("date", dateString);

        logger.info("guestId=" + updateInfo.getGuestId() + " objectType=weight" +
                    " connector=fitbit action=updateOneDayOfData date=" + dateString);
        apiDataService.eraseApiData(updateInfo.apiKey, weightOT, Arrays.asList(dateString));

        loadWeightDataForOneDay(updateInfo, date, userTimeZone, dateString);

        // If that succeeded, update where to start next time.  If scaleLastServerSyncMillis is not -1,
        // meaning we have a hardware scale, record the minimum of the end of the day "date" and scaleLastServerSyncMillis
        // as SCALE.lastSyncDate.  If scaleLastServerSyncMillis> the end of the day "date" then we already know
        // we have complete data for "date", and if we fail we should start with the day after that.
        // If scaleLastServerSyncMillis < the end of the day "date" then we have incomplete data for today
        // and will need to try it again next time.  If scaleLastServerSyncMillis is -1, we don't have a
        // hardware scale.  Just record the min of the end of this date and when we just asked.

        // Compute the start and end of that date in milliseconds for comparing
        // and truncating start/end
        DateTimeZone dateTimeZone = DateTimeZone.UTC;
        LocalDate localDate = LocalDate.parse(dateString);
        long dateEndMillis = localDate.toDateTimeAtStartOfDay(dateTimeZone).getMillis() + DateTimeConstants.MILLIS_PER_DAY;

        if(scaleLastServerSyncMillis==-1 || dateEndMillis< scaleLastServerSyncMillis) {
            guestService.setApiKeyAttribute(updateInfo.apiKey, "SCALE.lastSyncDate",
                                            String.valueOf(dateEndMillis));
        }
     }

    private void loadWeightDataForOneDay(UpdateInfo updateInfo, Date date, TimeZone timeZone, String formattedDate) throws Exception {
        String json = getWeightData(updateInfo, formattedDate);
        String fatJson = getBodyFatData(updateInfo, formattedDate);
        JSONObject jsonWeight = JSONObject.fromObject(json);
        JSONObject jsonFat = JSONObject.fromObject(fatJson);
        json = mergeWeightInfos(jsonWeight, jsonFat);
        long fromMidnight = TimeUtils.fromMidnight(date.getTime(), timeZone);
        long toMidnight = TimeUtils.toMidnight(date.getTime(), timeZone);
        logger.info("guestId=" + updateInfo.getGuestId() +
                    " connector=fitbit action=loadWeightDataForOneDay json="
                    + json);
        if (json != null) {
            apiDataService.cacheApiDataJSON(updateInfo, json, fromMidnight, toMidnight, weightOT.value());
        } else
            apiDataService.cacheEmptyData(updateInfo, fromMidnight, toMidnight);
    }

    private static String mergeWeightInfos(final JSONObject jsonWeight, final JSONObject jsonFat) {
        JSONArray weightArray = jsonWeight.getJSONArray("weight");
        JSONArray fatArray = jsonFat.getJSONArray("fat");
        for(int i=0; i<weightArray.size(); i++) {
            JSONObject weightJSON = weightArray.getJSONObject(i);
            long logId = weightJSON.getLong("logId");
            for(int j=0; j<fatArray.size(); j++) {
                JSONObject fatJSON = fatArray.getJSONObject(j);
                long otherLogId = fatJSON.getLong("logId");
                if (otherLogId==logId) {
                    double fat = fatJSON.getDouble("fat");
                    weightJSON.put("fat", fat);
                }
            }
        }
        return jsonWeight.toString();
    }

    private void loadActivityDataForOneDay(UpdateInfo updateInfo, Date date,
			TimeZone timeZone, String formattedDate) throws Exception {
		String json = getActivityData(updateInfo, formattedDate);
		long fromMidnight = TimeUtils.fromMidnight(date.getTime(), timeZone);
		long toMidnight = TimeUtils.toMidnight(date.getTime(), timeZone);
		logger.info("guestId=" + updateInfo.getGuestId() +
				" connector=fitbit action=loadActivityDataForOneDay json="
						+ json);
		if (json != null) {
			apiDataService.cacheApiDataJSON(updateInfo, json, fromMidnight, toMidnight,
                                            activityOT.value()+loggedActivityOT.value());
		}
	}

	private void loadSleepDataForOneDay(UpdateInfo updateInfo, Date date,
			TimeZone timeZone, String formattedDate) throws Exception {
		String json = getSleepData(updateInfo, formattedDate);
		long fromMidnight = TimeUtils.fromMidnight(date.getTime(), timeZone);
		long toMidnight = TimeUtils.toMidnight(date.getTime(), timeZone);
		if (json != null) {
			apiDataService.cacheApiDataJSON(updateInfo, json, fromMidnight,
					toMidnight, sleepOT.value());
		}
	}

	private String getSleepData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException {
		String urlString = "http://api.fitbit.com/1/user/-/sleep/date/"
				+ formattedDate + ".json";

		String json = signpostHelper.makeRestCall(updateInfo.apiKey, sleepOT.value(), urlString);

		return json;
	}

    private String getWeightData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException {
        String urlString = "http://api.fitbit.com/1/user/-/body/log/weight/date/"
                           + formattedDate + ".json";

        String json = signpostHelper.makeRestCall(updateInfo.apiKey, weightOT.value(), urlString);

        return json;
    }

    private String getBodyFatData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException {
        String urlString = "http://api.fitbit.com/1/user/-/body/log/fat/date/"
                           + formattedDate + ".json";

        String json = signpostHelper.makeRestCall(updateInfo.apiKey, weightOT.value(), urlString);

        return json;
    }

    private String getActivityData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException {

		String urlString = "http://api.fitbit.com/1/user/-/activities/date/"
				+ formattedDate + ".json";

		String json = signpostHelper.makeRestCall(updateInfo.apiKey, activityOT.value()+loggedActivityOT.value(), urlString);

		return json;
	}
    //
	//@RequestMapping("/fitbit/notify")
	//public void notifyMeasurement(@RequestBody String updatesString,
	//		HttpServletRequest request, HttpServletResponse response)
	//		throws Exception {
    //
	//	String lines[] = updatesString.split("\\r?\\n");
    //
	//	for (String line : lines) {
	//		if (line.startsWith("[{\"collectionType")) {
	//			updatesString = line;
	//			break;
	//		}
	//	}
    //
	//	logger.info("action=apiNotification connector=fitbit message="
	//			+ updatesString);
    //
	//	try {
	//		JSONArray updatesArray = JSONArray.fromObject(updatesString);
	//		for (int i = 0; i < updatesArray.size(); i++) {
	//			JSONObject jsonUpdate = updatesArray.getJSONObject(i);
	//			String collectionType = jsonUpdate.getString("collectionType");
	//			// warning: 'body' doesn't have a date!!!
	//			if (collectionType.equals("body"))
	//				continue;
	//			String dateString = jsonUpdate.getString("date");
	//			String ownerId = jsonUpdate.getString("ownerId");
	//			String subscriptionId = jsonUpdate.getString("subscriptionId");
    //
     //           FitbitUserProfile userProfile = jpaDaoService.findOne("fitbitUser.byEncodedId", FitbitUserProfile.class, ownerId);
    //
     //           long guestId = userProfile.guestId;
    //
     //           int objectTypes = 0;
	//			if (collectionType.equals("foods")
	//					|| collectionType.equals("body")) {
     //               //notificationsService.addExceptionNotification(guestId, Notification.Type.INFO, "Received new body info from Fitbit");
	//				continue;
	//			} else if (collectionType.equals("activities")) {
     //               //notificationsService.addExceptionNotification(guestId, Notification.Type.INFO, "Received new activity info from Fitbit");
     //               objectTypes = 3;
	//			} else if (collectionType.equals("sleep")) {
     //               //notificationsService.addExceptionNotification(guestId, Notification.Type.INFO, "Received new sleep info from Fitbit");
     //               objectTypes = 4;
	//			}
    //
	//			connectorUpdateService.addApiNotification(connector(),
	//					userProfile.guestId, updatesString);
    //
	//			JSONObject jsonParams = new JSONObject();
	//			jsonParams.accumulate("date", dateString)
	//					.accumulate("ownerId", ownerId)
	//					.accumulate("subscriptionId", subscriptionId);
    //
	//			logger.info("action=scheduleUpdate connector=fitbit collectionType="
	//					+ collectionType);
    //
	//			connectorUpdateService.scheduleUpdate(userProfile.guestId,
	//					connector().getName(), objectTypes,
	//					UpdateType.PUSH_TRIGGERED_UPDATE,
	//					System.currentTimeMillis() + 5000,
	//					jsonParams.toString());
	//		}
	//	} catch (Exception e) {
	//		System.out.println("error processing fitbit notification " + updatesString);
	//		e.printStackTrace();
	//		logger.warn("Could not parse fitbit notification: "
	//				+ Utils.stackTrace(e));
	//	}
	//}

    public static void main(final String[] args) {
        String weightStr = "{\"weight\":[{\"bmi\":30.37,\"date\":\"2013-06-09\",\"logId\":1370775582000,\"time\":\"10:59:42\",\"weight\":101.6},{\"bmi\":31,\"date\":\"2013-06-09\",\"logId\":1370822399000,\"time\":\"23:59:59\",\"weight\":103.7}]}";
        String fatStr = "{\"fat\":[{\"date\":\"2013-06-09\",\"fat\":27.63,\"logId\":1370775582000,\"time\":\"10:59:42\"}]}";
        mergeWeightInfos(JSONObject.fromObject(weightStr), JSONObject.fromObject(fatStr));
    }
}
