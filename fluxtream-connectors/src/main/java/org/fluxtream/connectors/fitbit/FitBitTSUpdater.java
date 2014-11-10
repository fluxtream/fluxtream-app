package org.fluxtream.connectors.fitbit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import org.apache.commons.io.IOUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Autonomous;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.*;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.AbstractLocalTimeFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.NotificationsService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.fluxtream.core.utils.Utils;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author candide
 * 
 */
@Component
@Controller
@Updater(prettyName = "Fitbit", value = 7, objectTypes = {
		FitbitTrackerActivityFacet.class, FitbitLoggedActivityFacet.class,
		FitbitSleepFacet.class, FitbitWeightFacet.class, FitbitFoodLogSummaryFacet.class,
        FitbitFoodLogEntryFacet.class},
        userProfile = FitbitUserProfile.class,
        bodytrackResponder = FitbitBodytrackResponder.class,
        defaultChannels = {"Fitbit.steps","Fitbit.caloriesOut", "Fitbit.loggedActivitesDistance", "caloriesIn"})
public class FitBitTSUpdater extends AbstractUpdater implements Autonomous {

    private static final String IS_GUEST_SUBSCRIBED_TO_FITBIT_NOTIFICATIONS_ATT_KEY = "isGuestSubscribedToFitbitNotifications";
    private static final String CALORIES_IN_HISTORY_IMPORTED_ATT_KEY = "caloriesInHistoryImported";
    private static final String HAS_FITBIT_USER_PROFILE_ATT_KEY = "hasFitbitUserProfile";
    private static final String BACKSYNC_DAYS_GOAL_ATT_KEY = "backsync.date.goal";
    private static final String BACKSYNC_START_DATE_ATT_KEY = "backsync.start.date";

    FlxLogger logger = FlxLogger.getLogger(FitBitTSUpdater.class);

    @Autowired
    FitbitPersistenceHelper fitbitPersistenceHelper;

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
    public static final String SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL = "SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL";

    final ObjectType sleepOT = ObjectType.getObjectType(connector(),
                                                          "sleep");
    final ObjectType weightOT = ObjectType.getObjectType(connector(),
                                                        "weight");
    final ObjectType activityOT = ObjectType.getObjectType(connector(),
                                                             "activity_summary");
    final ObjectType loggedActivityOT = ObjectType.getObjectType(
            connector(), "logged_activity");
    final ObjectType foodLogEntryOT = ObjectType.getObjectType(connector(),
            "food_log_entry");
    final ObjectType foodLogSummaryOT = ObjectType.getObjectType(connector(),
            "food_log_summary");

    static {
		ObjectType.registerCustomObjectType(GET_STEPS_CALL);
		ObjectType.registerCustomObjectType(GET_USER_PROFILE_CALL);
        ObjectType.registerCustomObjectType(GET_USER_DEVICES_CALL);
        ObjectType.registerCustomObjectType(SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL);
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
			throws Exception {

        // sleep

        loadTimeSeries("sleep/timeInBed", updateInfo, sleepOT,
                "timeInBed");
        loadTimeSeries("sleep/startTime", updateInfo, sleepOT,
                "startTime");
        loadTimeSeries("sleep/minutesAsleep", updateInfo, sleepOT,
                "minutesAsleep");
        loadTimeSeries("sleep/minutesAwake", updateInfo, sleepOT,
                "minutesAwake");
        loadTimeSeries("sleep/minutesToFallAsleep", updateInfo,
                sleepOT, "minutesToFallAsleep");
        loadTimeSeries("sleep/minutesAfterWakeup", updateInfo,
                sleepOT, "minutesAfterWakeup");
        loadTimeSeries("sleep/awakeningsCount", updateInfo, sleepOT, "awakeningsCount");

        // activities

        loadTimeSeries("activities/tracker/calories", updateInfo,
                activityOT, "caloriesOut");
        loadTimeSeries("activities/tracker/steps", updateInfo,
                activityOT, "steps");
        loadTimeSeries("activities/tracker/distance", updateInfo,
                activityOT, "totalDistance");

        // The floors and elevation APIs report 400 errors if called on
        // an account which has never been bound to a Fitbit device which
        // has an altimeter, such as the Fitbit Ultra.  For now, disable
        // reading these APIs.  In the future, perhaps check the device
        // type and conditionally call these APIs.
        loadTimeSeries("activities/tracker/floors", updateInfo,
                activityOT, "floors");
        loadTimeSeries("activities/tracker/elevation", updateInfo,
                activityOT, "elevation");
        loadTimeSeries("activities/tracker/minutesSedentary",
                updateInfo, activityOT, "sedentaryMinutes");
        loadTimeSeries("activities/tracker/minutesLightlyActive",
                updateInfo, activityOT, "lightlyActiveMinutes");
        loadTimeSeries("activities/tracker/minutesFairlyActive",
                updateInfo, activityOT, "fairlyActiveMinutes");
        loadTimeSeries("activities/tracker/minutesVeryActive",
                updateInfo, activityOT, "veryActiveMinutes");
        loadTimeSeries("activities/tracker/activeScore", updateInfo,
                activityOT, "activeScore");
        loadTimeSeries("activities/tracker/activityCalories",
                updateInfo, activityOT, "activityCalories");

        // weight
        // Store the time when we're asking about the weight in case this
        // account doesn't have a hardware scale associated with it
        long weightRequestMillis = System.currentTimeMillis();

        loadTimeSeries("body/weight", updateInfo, weightOT,
                "weight");
        loadTimeSeries("body/bmi", updateInfo, weightOT,
                "bmi");
        loadTimeSeries("body/fat", updateInfo, weightOT,
                "fat");

        jpaDaoService.execute("DELETE FROM Facet_FitbitSleep sleep WHERE sleep.start=0");
        final JSONArray deviceStatusesArray = getDeviceStatusesArray(updateInfo);

        // Store TRACKER.lastSyncDate
        long trackerLastSyncDate = -1;

        try {
            trackerLastSyncDate = getLastServerSyncMillis(deviceStatusesArray, "TRACKER");
        } catch (Throwable t) {
            logger.info("guestId=" + updateInfo.getGuestId() +
                    " connector=fitbit action=updateConnectorDataHistory " +
                    " message=\"Error getting TRACKER.lastSyncDate\" stackTrace=<![CDATA[\"" + t.getStackTrace() + "]]>");
        }

        if (trackerLastSyncDate == -1) {
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
        if (scaleLastSyncDate == -1) {
            guestService.setApiKeyAttribute(updateInfo.apiKey, "SCALE.lastSyncDate",
                    String.valueOf(weightRequestMillis));
        } else {
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

        checkLateAdditions(updateInfo);

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

	public void loadTimeSeries(String uri, UpdateInfo updateInfo,
			ObjectType objectType, String fieldName)
			throws RateLimitReachedException {

        String json = "";
        try {
            json = makeRestCall(updateInfo,
                    uri.hashCode(), "https://api.fitbit.com/1/user/-/" + uri
                            + "/date/today/max.json");
        } catch (Throwable t) {
            // elevation and floors are not available for earlier trackers, so we can safely ignore them
            if (fieldName.equals("elevation")||fieldName.equals("floors")) {
                logger.info("guestId=" + updateInfo.apiKey.getGuestId() +
                            " connector=fitbit action=loadTimeSeries message=\"Could not load timeseries for " + fieldName);
                return;
            }
        }


		JSONObject timeSeriesJson;
        try {
            timeSeriesJson = JSONObject.fromObject(json);
        } catch (Throwable t) {
            logger.warn("Could not load time series, objectType=" + objectType.getName() + ", fieldName=" + fieldName);
            return;
        }
 		String resourceName = uri.replace('/', '-');
		JSONArray timeSeriesArray = timeSeriesJson.getJSONArray(resourceName);
		for (int i = 0; i < timeSeriesArray.size(); i++) {
            JSONObject entry = timeSeriesArray.getJSONObject(i);
            String date = entry.getString("dateTime");


            if (objectType == sleepOT) {
                FitbitSleepFacet facet = getSleepFacet(updateInfo.apiKey.getId(),
                                                       date);
                if (facet == null) {
                    facet = new FitbitSleepFacet(updateInfo.apiKey.getId());
                    facet.isEmpty = true;
                    facet.date = date;
                    facet.api = connector().value();
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facetDao.persist(facet);
                }
                addToSleepFacet(facet, entry, fieldName);
            } else if (objectType == activityOT) {
                FitbitTrackerActivityFacet facet = getActivityFacet(
                        updateInfo.apiKey.getId(), date);
                if (facet == null) {
                    facet = new FitbitTrackerActivityFacet(updateInfo.apiKey.getId());
                    setCommonFacetProperties(updateInfo, date, facet);
                    facetDao.persist(facet);
                }
                addToActivityFacet(facet, entry, fieldName);
            } else if (objectType == weightOT) {
                FitbitWeightFacet facet = getWeightFacet(updateInfo.apiKey.getId(), date);
                if (facet == null) {
                    facet = new FitbitWeightFacet(updateInfo.apiKey.getId());
                    setCommonFacetProperties(updateInfo, date, facet);

                    facetDao.persist(facet);
                }
                addToWeightFacet(facet, entry, fieldName);
            } else if (objectType == foodLogSummaryOT) {
                FitbitFoodLogSummaryFacet facet = getFoodLogSummaryFacet(updateInfo.apiKey.getId(), date);
                if (facet == null) {
                    facet = new FitbitFoodLogSummaryFacet(updateInfo.apiKey.getId());
                    setCommonFacetProperties(updateInfo, date, facet);
                }
                setFieldValue(facet, fieldName, entry.getString("value"));
                facetDao.merge(facet);
            }
		}
	}

    private void setCommonFacetProperties(UpdateInfo updateInfo, String date, AbstractLocalTimeFacet facet) {
        facet.date = date;
        facet.api = connector().value();
        facet.guestId = updateInfo.apiKey.getGuestId();

        final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(date);

        facet.start = dateTime.getMillis();
        facet.end = dateTime.getMillis() + DateTimeConstants.MILLIS_PER_DAY - 1;

        facet.startTimeStorage = date + "T00:00:00.000";
        facet.endTimeStorage = date + "T23:59:59.999";
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

    private FitbitFoodLogSummaryFacet getFoodLogSummaryFacet(final long apiKeyId, String date) {
        return jpaDaoService.findOne("fitbit.foodLog.summary.byDate",
                FitbitFoodLogSummaryFacet.class, apiKeyId, date);
    }

	@Transactional(readOnly = false)
	private void addToSleepFacet(FitbitSleepFacet facet, JSONObject entry,
			String fieldName) {
		if (fieldName.equals("startTime")) {
			storeTime(entry.getString("value"), facet);
            // let's delete empty sleep entries as soon as we can
            if (facet.start==0)
                facetDao.delete(facet);
            else {
                facet.isEmpty = false;
                facetDao.merge(facet);
            }
		} else {
            setFieldValue(facet, fieldName, entry.getString("value"));
            facetDao.merge(facet);
        }
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
            return getListOfDatesBetween(updateInfo, lastStoredSyncMillis, trackerLastServerSyncMillis);
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

            return getListOfDatesBetween(updateInfo, lastStoredSyncMillis, endMillis);
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

    private List<String> getListOfDatesBetween(final UpdateInfo updateInfo, final long startMillis, final long endMillis) {
        List<String> dates = new ArrayList<String>();
        // TODO: what really matters here is the date that the Fitbit server uses for the
        // tracker update times, so this isn't quite right
        final DateTimeZone zone = getFitbitUserTimezone(updateInfo);
        final LocalDate startDate = new LocalDate(startMillis, zone);
        // in order to have the date of endMillis in the resulting list we need to add a
        // a day
        final long dayAfterEnd = endMillis + DateTimeConstants.MILLIS_PER_DAY;
        int days = Days.daysBetween(startDate, new LocalDate(dayAfterEnd, zone)).getDays();
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.withFieldAdded(DurationFieldType.days(), i);
            String dateString = TimeUtils.dateFormatter.print(d.toDateTimeAtStartOfDay().getMillis());
            dates.add(dateString);
        }
        return dates;
    }

    private JSONArray getDeviceStatusesArray(final UpdateInfo updateInfo)
            throws RateLimitReachedException, UpdateFailedException, AuthExpiredException, UnexpectedResponseCodeException {
        String urlString = "https://api.fitbit.com/1/user/-/devices.json";

        final ObjectType customObjectType = ObjectType.getCustomObjectType(GET_USER_DEVICES_CALL);
        final int getUserDevicesObjectTypeID = customObjectType.value();
        String json = makeRestCall(updateInfo, getUserDevicesObjectTypeID,
                urlString);
        return JSONArray.fromObject(json);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        checkLateAdditions(updateInfo);

        if (updateInfo.getUpdateType()==UpdateInfo.UpdateType.PUSH_TRIGGERED_UPDATE) {
            JSONObject jsonParams = JSONObject.fromObject(updateInfo.jsonParams);
            String formattedDate = jsonParams.getString("date");
            switch(updateInfo.objectTypes) {
                case 3: // activities
                    loadActivityDataForOneDay(updateInfo, formattedDate);
                    final List<AbstractFacet> facetsByDates = facetDao.getFacetsByDates(updateInfo.apiKey, activityOT, Arrays.asList(formattedDate));
                    if (facetsByDates.size()>0) {
                        final FitbitTrackerActivityFacet activityFacet = (FitbitTrackerActivityFacet) facetsByDates.get(0);
                        updateIntradayMetrics(updateInfo, activityFacet);
                    }
                    break;
                case 4: // sleep
                    loadSleepDataForOneDay(updateInfo, formattedDate);
                    break;
                case 8: // body
                    final JSONArray deviceStatusesArray = getDeviceStatusesArray(updateInfo);
                    // will return -1 if there is no scale in the devicesStatuses, which is interpreted as
                    // a manual weight entry in the updateOneDayOfScaleData method
                    final long scaleLastServerSyncMillis = getLastServerSyncMillis(deviceStatusesArray, "SCALE");
                    updateOneDayOfScaleData(updateInfo, formattedDate, scaleLastServerSyncMillis);
                    break;
                case 16+32: // foods
                    loadFoodDataForOneDay(updateInfo, formattedDate);
                    break;
            }
            return;
        }

        final JSONArray deviceStatusesArray = getDeviceStatusesArray(updateInfo);
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

        // now let's backsync logged activities, sleep and food which can be manually entered and potentially
        // not caught by the subscription/notification mechanism
        // note: we know that updateInfo's remainingAPICalls property has been set since there is always a mandatory
        // call to getDeviceStatusesArray() (https://api.fitbit.com/1/user/-/devices.json)
        // We are incrementally going farther back in time up to a given backfill limit ("fitbit.backsync.days"),
        // keeping track of up to when we've got to and starting from there next time
        LocalDate startDate = getBackSyncStartDate(updateInfo);
        String backSyncDateGoal = getBackSyncDateGoal(updateInfo);

        while (updateInfo.getRemainingAPICalls("fitbit")>50) {
            String formattedDate = TimeUtils.dateFormatter.print(startDate);
            System.out.println("backsynching, we have " + updateInfo.getRemainingAPICalls("fitbit") +
                    " API calls left, apiKeyId=" + updateInfo.apiKey.getId() + ", startDate=" + formattedDate + ", goal=" + backSyncDateGoal);
            loadActivityDataForOneDay(updateInfo, formattedDate); if (updateInfo.getRemainingAPICalls("fitbit")<=50) break;
            loadSleepDataForOneDay(updateInfo, formattedDate); if (updateInfo.getRemainingAPICalls("fitbit")<=50) break;
            loadFoodDataForOneDay(updateInfo, formattedDate); if (updateInfo.getRemainingAPICalls("fitbit")<=50) break;
            loadIntradayDataForOneDay(updateInfo, formattedDate);
            // start over when we have reached our goal
            if (isGoalReached(startDate, backSyncDateGoal)) {
                guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), BACKSYNC_START_DATE_ATT_KEY);
                guestService.removeApiKeyAttribute(updateInfo.apiKey.getId(), BACKSYNC_DAYS_GOAL_ATT_KEY);
                startDate = getBackSyncStartDate(updateInfo);
                backSyncDateGoal = getBackSyncDateGoal(updateInfo);
            } else {
                startDate = startDate.minusDays(1);
                // remember up to when we were able to sync back so far
                guestService.setApiKeyAttribute(updateInfo.apiKey, BACKSYNC_START_DATE_ATT_KEY, TimeUtils.dateFormatter.print(startDate));
            }
        }
	}

    public void afterHistoryUpdate(final UpdateInfo updateInfo) {
        scheduleAggressiveBackSync(updateInfo, System.currentTimeMillis());
    }

    public void afterConnectorUpdate(final UpdateInfo updateInfo) {
        scheduleAggressiveBackSync(updateInfo, System.currentTimeMillis() + DateTimeConstants.MILLIS_PER_HOUR);
    }

    // This allows to exceptionnally override the standard update scheduling mechanism: fitbit doesn't support updatedSince
    // semantics meaning we have to be aggressive about synching
    private void scheduleAggressiveBackSync(final UpdateInfo updateInfo, final long when) {
        System.out.println("scheduling next backsynching operations , apiKeyId=" + updateInfo.apiKey.getId());
        connectorUpdateService.scheduleUpdate(updateInfo.apiKey, 0, UpdateInfo.UpdateType.INCREMENTAL_UPDATE,
                when);
        initChannelMapping(updateInfo);
    }

    static boolean isGoalReached(LocalDate startDate, String backSyncDateGoal) {
        LocalDate backSyncDate = new LocalDate(backSyncDateGoal);
        if (backSyncDate.isEqual(startDate)||backSyncDate.isAfter(startDate))
            return true;
        return false;
    }

    private LocalDate getBackSyncStartDate(final UpdateInfo updateInfo) {
        // return either today (and store it as an ApiKeyAttribute) or what we have already saved from a previous run
        final String backSyncStartDateAtt = guestService.getApiKeyAttribute(updateInfo.apiKey, BACKSYNC_START_DATE_ATT_KEY);
        if (backSyncStartDateAtt!=null) {
            return new LocalDate(backSyncStartDateAtt);
        } else {
            DateTimeZone zone = getFitbitUserTimezone(updateInfo);
            LocalDate backSyncStartDate = new LocalDate(zone);
            guestService.setApiKeyAttribute(updateInfo.apiKey, BACKSYNC_START_DATE_ATT_KEY, TimeUtils.dateFormatter.print(backSyncStartDate));
            return backSyncStartDate;
        }
    }

    private String getBackSyncDateGoal(final UpdateInfo updateInfo) {
        // either use the date we had computed on a previous run or subtract 'fitbit.backsync.days' from today and
        // store that as an ApiKeyAttribute
        String backSyncDateGoal = guestService.getApiKeyAttribute(updateInfo.apiKey, BACKSYNC_DAYS_GOAL_ATT_KEY);
        if (backSyncDateGoal==null) {
            int fitbitBacksyncDays = 30;
            final String fitbitBacksyncDaysProperty = env.get("fitbit.backsync.days");
            if (fitbitBacksyncDaysProperty!=null)
                fitbitBacksyncDays = Integer.valueOf(fitbitBacksyncDaysProperty);
            DateTimeZone fitbitUserTimezone = getFitbitUserTimezone(updateInfo);
            LocalDate today = new LocalDate(fitbitUserTimezone);
            String computedBackSyncDateGoal = ISODateTimeFormat.date().print(today.minusDays(fitbitBacksyncDays));
            String memberSince = getMemberSince(updateInfo);
            backSyncDateGoal = mostRecentDate(computedBackSyncDateGoal, memberSince);
            guestService.setApiKeyAttribute(updateInfo.apiKey, BACKSYNC_DAYS_GOAL_ATT_KEY, backSyncDateGoal);
        }
        return backSyncDateGoal;
    }

    private String mostRecentDate(String computedBackSyncDateGoal, String memberSince) {
        LocalDate computedDate = ISODateTimeFormat.date().parseLocalDate(computedBackSyncDateGoal);
        LocalDate memberSinceDate = ISODateTimeFormat.date().parseLocalDate(memberSince);
        return computedDate.isAfter(memberSinceDate)
                ? computedBackSyncDateGoal
                : memberSince;
    }

    private String getMemberSince(UpdateInfo updateInfo) {
        FitbitUserProfile userProfile = jpaDaoService.findOne("fitbitUser.byApiKeyId", FitbitUserProfile.class, updateInfo.apiKey.getId());
        return userProfile.memberSince;
    }

    private DateTimeZone getFitbitUserTimezone(UpdateInfo updateInfo) {
        DateTimeZone zone = DateTimeZone.UTC;
        FitbitUserProfile userProfile = jpaDaoService.findOne("fitbitUser.byApiKeyId", FitbitUserProfile.class, updateInfo.apiKey.getId());
        if (userProfile!=null)
            try { zone = DateTimeZone.forID(userProfile.timezone);} catch(Exception e){}
        return zone;
    }

    private boolean isIntradayDataEnabled() {
        final String intradayEnabledProperty = env.get("fitbit.intraday.enabled");
        if (intradayEnabledProperty !=null && Boolean.valueOf(intradayEnabledProperty))
            return true;
        return false;
    }

    private void checkLateAdditions(UpdateInfo updateInfo) throws RateLimitReachedException, UpdateFailedException, AuthExpiredException, NoSuchFieldException, IllegalAccessException, UnexpectedResponseCodeException {

        // 10/27/2014 Adding support for food logging. This requires Fitbit's User info to be persisted
        // so we can retrieve a guest ID with Fitbit's 'encodedId' hash
        maybeRetrieveFitbitUserInfo(updateInfo);

        // Also, food logging requires that the user subscribe to fitbit's notifications
        maybeSubscribeToFitbitNotifications(updateInfo);

        // We want to fill-in the historical caloriesIn data
        maybeImportCaloriesInHistory(updateInfo);
    }

    private void maybeImportCaloriesInHistory(UpdateInfo updateInfo) throws RateLimitReachedException {
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, CALORIES_IN_HISTORY_IMPORTED_ATT_KEY)!=null)
            return;
        loadTimeSeries("foods/log/caloriesIn", updateInfo, foodLogSummaryOT,
                "calories");
        // add in water intake for good measure
        loadTimeSeries("foods/log/water", updateInfo, foodLogSummaryOT,
                "water");
        bodyTrackStorageService.storeInitialHistory(updateInfo.apiKey, foodLogSummaryOT.value());
        guestService.setApiKeyAttribute(updateInfo.apiKey, CALORIES_IN_HISTORY_IMPORTED_ATT_KEY, String.valueOf(true));
    }

    private void maybeSubscribeToFitbitNotifications(UpdateInfo updateInfo) throws UpdateFailedException, RateLimitReachedException, AuthExpiredException {
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, IS_GUEST_SUBSCRIBED_TO_FITBIT_NOTIFICATIONS_ATT_KEY)!=null)
            return;

        try {
            String fitbitSubscriberId = env.get("fitbitSubscriberId");
            makeRestCall(updateInfo, ObjectType.getCustomObjectType(SUBSCRIBE_TO_FITBIT_NOTIFICATIONS_CALL).value(),
                    "https://api.fitbit.com/1/user/-/apiSubscriptions/" + fitbitSubscriberId + ".json", "POST");
        } catch (UnexpectedResponseCodeException e) {
            if (e.responseCode==409) {
                notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING,
                        "Subscription Conflict", "It looks like you are already subscribed to notifications with this key on another server (http error code: " + e.responseCode + ")");
            }
            logger.warn("Could not subscribe guest " + updateInfo.getGuestId() + " to fitbit notifications (Fitbit API's HTTP code: " + e.responseCode + ")");
            return;
        }

        guestService.setApiKeyAttribute(updateInfo.apiKey, IS_GUEST_SUBSCRIBED_TO_FITBIT_NOTIFICATIONS_ATT_KEY, String.valueOf(true));
    }

    private void maybeRetrieveFitbitUserInfo(UpdateInfo updateInfo) throws AuthExpiredException, RateLimitReachedException, UpdateFailedException, UnexpectedResponseCodeException {
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, HAS_FITBIT_USER_PROFILE_ATT_KEY)!=null)
            return;
        final String userProfileJson = makeRestCall(updateInfo, ObjectType.getCustomObjectType(GET_USER_PROFILE_CALL).value(),
                "https://api.fitbit.com/1/user/-/profile.json");
        JSONObject json = JSONObject.fromObject(userProfileJson);
        if (!json.has("user")) {
            logger.warn("No Fitbit user profile, retrieved json: " + userProfileJson);
            return;
        }
        JSONObject user = json.getJSONObject("user");
        FitbitUserProfile fitbitUserProfile = new FitbitUserProfile();
        fitbitUserProfile.encodedId = user.getString("encodedId");
        fitbitUserProfile.apiKeyId = updateInfo.apiKey.getId();
        if (user.has("aboutMe"))
            fitbitUserProfile.aboutMe = user.getString("aboutMe");
        if (user.has("city"))
            fitbitUserProfile.city = user.getString("city");
        if (user.has("country"))
            fitbitUserProfile.country = user.getString("country");
        if (user.has("dateOfBirth"))
            fitbitUserProfile.dateOfBirth = user.getString("dateOfBirth");
        if (user.has("displayName"))
            fitbitUserProfile.displayName = user.getString("displayName");
        if (user.has("fullName"))
            fitbitUserProfile.fullName = user.getString("fullName");
        if (user.has("gender"))
            fitbitUserProfile.gender = user.getString("gender");
        if (user.has("height"))
            fitbitUserProfile.height = user.getDouble("height");
        if (user.has("nickname"))
            fitbitUserProfile.nickname = user.getString("nickname");
        if (user.has("offsetFromUTCMillis"))
            fitbitUserProfile.offsetFromUTCMillis = user.getLong("offsetFromUTCMillis");
        if (user.has("state"))
            fitbitUserProfile.state = user.getString("state");
        if (user.has("strideLengthRunning"))
            fitbitUserProfile.strideLengthRunning = user.getDouble("strideLengthRunning");;
        if (user.has("strideLengthWalking"))
            fitbitUserProfile.strideLengthWalking = user.getDouble("strideLengthWalking");
        if (user.has("timezone"))
            fitbitUserProfile.timezone = user.getString("timezone");
        if (user.has("weight"))
            fitbitUserProfile.weight = user.getDouble("weight");

        if (user.has("memberSince"))
            fitbitUserProfile.memberSince = user.getString("memberSince");
        if (user.has("weightUnit"))
            fitbitUserProfile.weightUnit = user.getString("weightUnit");
        if (user.has("heightUnit"))
            fitbitUserProfile.heightUnit = user.getString("heightUnit");
        if (user.has("glucoseUnit"))
            fitbitUserProfile.glucoseUnit = user.getString("glucoseUnit");
        if (user.has("waterUnit"))
            fitbitUserProfile.waterUnit = user.getString("waterUnit");
        if (user.has("avatar"))
            fitbitUserProfile.avatar = user.getString("avatar");
        if (user.has("avatar150"))
            fitbitUserProfile.avatar150 = user.getString("avatar150");
        if (user.has("startDayOfWeek"))
            fitbitUserProfile.startDayOfWeek = user.getString("startDayOfWeek");

        facetDao.persist(fitbitUserProfile);
        guestService.setApiKeyAttribute(updateInfo.apiKey, HAS_FITBIT_USER_PROFILE_ATT_KEY, String.valueOf(true));
    }

    private void loadIntradayDataForOneDay(UpdateInfo updateInfo, String dateString)
            throws AuthExpiredException, RateLimitReachedException, UpdateFailedException, NoSuchFieldException, IllegalAccessException, UnexpectedResponseCodeException {
        if (!isIntradayDataEnabled()) return;
        final List<AbstractFacet> facetsByDates = facetDao.getFacetsByDates(updateInfo.apiKey, ObjectType.getObjectType(connector(), 1), Arrays.asList(dateString));
        if (facetsByDates.size()>0) {
            final AbstractFacet facet = facetsByDates.get(0);
            if (facet!=null) {
                FitbitTrackerActivityFacet activityFacet = (FitbitTrackerActivityFacet) facet;
                updateIntradayMetrics(updateInfo, activityFacet);
                bodyTrackStorageService.storeApiData(updateInfo.getGuestId(), Arrays.asList(facet));
            } else {
                logger.warn("TRYING TO UPDATE INTRADAY DATA OF AN UNEXISTING FACET, dateString=" + dateString);
            }
        }
    }

    private void updateIntradayMetrics(UpdateInfo updateInfo, FitbitTrackerActivityFacet activityFacet) throws RateLimitReachedException, AuthExpiredException, UpdateFailedException, UnexpectedResponseCodeException {
        try {
            List<String> wantedMetrics = Arrays.asList("steps", "calories", "distance", "floors", "elevation");
            final Object wantedMetricsProperty = env.oauth.getProperty("fitbit.intraday.metrics.wanted");
            if (wantedMetricsProperty!=null&&wantedMetricsProperty instanceof List) {
                List<String> wantedMetricsConfig = (List<String>) wantedMetricsProperty;
                if (wantedMetricsConfig.size()>0)
                    wantedMetrics = wantedMetricsConfig;
            }
            for (String metric : wantedMetrics) {
                updateIntradayMetric(activityFacet, updateInfo, metric);
            }
        // following exception should never happen
        } catch (IllegalAccessException e) {
            throw new UpdateFailedException();
        } catch (NoSuchFieldException e) {
            throw new UpdateFailedException();
        }
    }


    public void updateIntradayMetric(final FitbitTrackerActivityFacet facet, final UpdateInfo updateInfo, final String metric)
            throws RateLimitReachedException, AuthExpiredException, UpdateFailedException, IllegalAccessException, NoSuchFieldException, UnexpectedResponseCodeException {
        Field field = FitbitTrackerActivityFacet.class.getField(metric + "Json");
        if (facet.date != null) {
            String json = makeRestCall(updateInfo,
                    String.format("activities/log/%s/date", metric).hashCode(),
                    String.format("https://api.fitbit.com/1/user/-/activities/log/%s/date/", metric)
                            + facet.date + "/1d.json");
            field.set(facet, json);
            facetDao.merge(facet);
        } else {
            logger.warn("guestId=" + updateInfo.getGuestId() +
                    " connector=fitbit action=updateIntradayMetric metric=" + metric + "message=facet date is null");
        }
    }

    private void updateTrackerListOfDays(final UpdateInfo updateInfo,
                                         final List<String> trackerDaysToSync, final long trackerLastServerSyncMillis) throws Exception {
        for (String dateString : trackerDaysToSync)
            updateOneDayOfTrackerData(updateInfo, dateString, trackerLastServerSyncMillis);
    }

    private void updateScaleListOfDays(final UpdateInfo updateInfo,
                                       final List<String> scaleDaysToSync, final long scaleLastServerSyncMillis) throws Exception {
        for (String dateString : scaleDaysToSync)
            updateOneDayOfScaleData(updateInfo, dateString, scaleLastServerSyncMillis);
    }

    private void updateOneDayOfTrackerData(final UpdateInfo updateInfo,
                                           final String dateString, final long trackerLastServerSyncMillis) throws Exception {
        updateInfo.setContext("date", dateString);

        logger.info("guestId=" + updateInfo.getGuestId() + " objectType=sleep" +
                    " connector=fitbit action=updateOneDayOfData date=" + dateString);
        apiDataService.eraseApiData(updateInfo.apiKey, sleepOT,
                                    Arrays.asList(dateString));
        loadSleepDataForOneDay(updateInfo, dateString);

        logger.info("guestId=" + updateInfo.getGuestId() + " objectType=activity" +
                    " connector=fitbit action=updateOneDayOfData date=" + dateString);
        apiDataService.eraseApiData(updateInfo.apiKey, activityOT, Arrays.asList(dateString));
        apiDataService.eraseApiData(updateInfo.apiKey, loggedActivityOT,
                                    Arrays.asList(dateString));

        loadActivityDataForOneDay(updateInfo, dateString);

        loadIntradayDataForOneDay(updateInfo, dateString);

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
                                         final String dateString, final long scaleLastServerSyncMillis) throws Exception {
        updateInfo.setContext("date", dateString);

        logger.info("guestId=" + updateInfo.getGuestId() + " objectType=weight" +
                    " connector=fitbit action=updateOneDayOfData date=" + dateString);
        apiDataService.eraseApiData(updateInfo.apiKey, weightOT, Arrays.asList(dateString));

        loadWeightDataForOneDay(updateInfo, dateString);

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

    private void loadWeightDataForOneDay(UpdateInfo updateInfo, String formattedDate) throws Exception {
        String json = getWeightData(updateInfo, formattedDate);
        String fatJson = getBodyFatData(updateInfo, formattedDate);
        JSONObject jsonWeight = JSONObject.fromObject(json);
        JSONObject jsonFat = JSONObject.fromObject(fatJson);
        json = mergeWeightInfos(jsonWeight, jsonFat);
        logger.info("guestId=" + updateInfo.getGuestId() +
                    " connector=fitbit action=loadWeightDataForOneDay json="
                    + json);
        apiDataService.eraseApiData(updateInfo.apiKey, weightOT, Arrays.asList(formattedDate));
        if (json != null) {
            JSONObject fitbitResponse = JSONObject.fromObject(json);
            final List<FitbitWeightFacet> createdOrUpdatedWeightFacets = fitbitPersistenceHelper.createOrUpdateWeightFacets(updateInfo, fitbitResponse);
            bodyTrackStorageService.storeApiData(updateInfo.getGuestId(), createdOrUpdatedWeightFacets);
        }
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

    private void loadActivityDataForOneDay(UpdateInfo updateInfo, String formattedDate) throws Exception {
        updateInfo.setContext("date", formattedDate);
		String json = getActivityData(updateInfo, formattedDate);
		logger.info("guestId=" + updateInfo.getGuestId() +
				" connector=fitbit action=loadActivityDataForOneDay json=" + json);
		if (json != null) {
            JSONObject fitbitResponse = JSONObject.fromObject(json);
            fitbitPersistenceHelper.createOrUpdateLoggedActivities(updateInfo, fitbitResponse);
            final FitbitTrackerActivityFacet createdOrUpdatedActivitySummary = fitbitPersistenceHelper.createOrUpdateActivitySummary(updateInfo, fitbitResponse);
            bodyTrackStorageService.storeApiData(updateInfo.getGuestId(), Arrays.asList(createdOrUpdatedActivitySummary));
		}
	}

	private void loadSleepDataForOneDay(UpdateInfo updateInfo, String formattedDate) throws Exception {
        updateInfo.setContext("date", formattedDate);
		String json = getSleepData(updateInfo, formattedDate);
        apiDataService.eraseApiData(updateInfo.apiKey, sleepOT, Arrays.asList(formattedDate));
		if (json != null) {
            JSONObject fitbitResponse = JSONObject.fromObject(json);
            fitbitPersistenceHelper.createOrUpdateSleepFacets(updateInfo, fitbitResponse);
		}
	}

    private void loadFoodDataForOneDay(UpdateInfo updateInfo, String formattedDate) throws Exception {
        updateInfo.setContext("date", formattedDate);
        final int objectTypes = foodLogEntryOT.value() + foodLogSummaryOT.value();
        String urlString = "https://api.fitbit.com/1/user/-/foods/log/date/"
                + formattedDate + ".json";
        String json = makeRestCall(updateInfo, objectTypes, urlString);
        apiDataService.eraseApiData(updateInfo.apiKey, foodLogEntryOT, Arrays.asList(formattedDate));
        apiDataService.eraseApiData(updateInfo.apiKey, foodLogSummaryOT, Arrays.asList(formattedDate));
        if (json != null) {
            JSONObject fitbitResponse = JSONObject.fromObject(json);
            JSONArray foodEntries = fitbitResponse.getJSONArray("foods");

            if (foodEntries == null || foodEntries.size() == 0)
                return;

            Iterator iterator = foodEntries.iterator();
            // use createOrUpdate for food log entries (can't rely on date boundaries for unicity)
            while (iterator.hasNext()) {
                JSONObject foodEntry = (JSONObject) iterator.next();
                fitbitPersistenceHelper.createOrUpdateFoodEntry(updateInfo, foodEntry);
            }

            final FitbitFoodLogSummaryFacet createdOrUpdatedFoodLogSummaryFacet = fitbitPersistenceHelper.createOrUpdateFoodLogSummaryFacet(updateInfo, fitbitResponse);
            bodyTrackStorageService.storeApiData(updateInfo.getGuestId(), Arrays.asList(createdOrUpdatedFoodLogSummaryFacet));
        }
    }

    private String getSleepData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException, UpdateFailedException, AuthExpiredException {
		String urlString = "https://api.fitbit.com/1/user/-/sleep/date/"
				+ formattedDate + ".json";

		String json = makeRestCall(updateInfo, sleepOT.value(), urlString);

		return json;
	}

    private String getWeightData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException, UpdateFailedException, AuthExpiredException {
        String urlString = "https://api.fitbit.com/1/user/-/body/log/weight/date/"
                           + formattedDate + ".json";

        String json = makeRestCall(updateInfo, weightOT.value(), urlString);

        return json;
    }

    private String getBodyFatData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException, UpdateFailedException, AuthExpiredException {
        String urlString = "https://api.fitbit.com/1/user/-/body/log/fat/date/"
                           + formattedDate + ".json";

        String json = makeRestCall(updateInfo, weightOT.value(), urlString);

        return json;
    }

    private String getActivityData(UpdateInfo updateInfo, String formattedDate)
            throws RateLimitReachedException, UnexpectedResponseCodeException, UpdateFailedException, AuthExpiredException {

		String urlString = "https://api.fitbit.com/1/user/-/activities/date/"
				+ formattedDate + ".json";

		String json = makeRestCall(updateInfo, activityOT.value() + loggedActivityOT.value(), urlString);

		return json;
	}

	@RequestMapping("/fitbit/notify")
	public void notifyMeasurement(@RequestBody String updatesString)
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

                FitbitUserProfile userProfile = jpaDaoService.findOne("fitbitUser.byEncodedId", FitbitUserProfile.class, ownerId);

                int objectTypes = 0;
				if (collectionType.equals("activities")) {
                    objectTypes = 3;
				} else if (collectionType.equals("sleep")) {
                    objectTypes = 4;
                } else if (collectionType.equals("body")) {
                        objectTypes = 8;
                } else if (collectionType.equals("foods")) {
                    objectTypes = 16+32;
                }

                ApiKey apiKey = guestService.getApiKey(userProfile.apiKeyId);
				connectorUpdateService.addApiNotification(connector(),
						apiKey.getGuestId(), updatesString);

				JSONObject jsonParams = new JSONObject();
				jsonParams.accumulate("date", dateString)
						.accumulate("ownerId", ownerId)
						.accumulate("subscriptionId", subscriptionId);

				logger.info("action=scheduleUpdate connector=fitbit collectionType="
						+ collectionType);

				connectorUpdateService.scheduleUpdate(apiKey,
						objectTypes,
						UpdateInfo.UpdateType.PUSH_TRIGGERED_UPDATE,
						System.currentTimeMillis(),
						jsonParams.toString());
			}
		} catch (Exception e) {
			System.out.println("error processing fitbit notification " + updatesString);
			e.printStackTrace();
			logger.warn("Could not parse fitbit notification: "
					+ Utils.stackTrace(e));
		}
	}

    public final String makeRestCall(final UpdateInfo updateInfo,
                                     final int objectTypes, final String urlString, final String...method)
            throws RateLimitReachedException, UpdateFailedException, AuthExpiredException, UnexpectedResponseCodeException {

        // if have already called the API from within this thread, the allowed remaining API calls will be saved
        // in the updateInfo
        final Integer remainingAPICalls = updateInfo.getRemainingAPICalls("fitbit");
        if (remainingAPICalls==null) {
            // otherwise, it means this is the first time we are calling the API
            // from within this update. It is possible that a previous update has consumed the entire API quota.
            // In this case, it has saved Fitbit's reset time as an ApiKey attribute. If however, this is the
            // very first API call for this connector, we are most probably not rate limited and so we can continue.
            String apiKeyAttResetTime = guestService.getApiKeyAttribute(updateInfo.apiKey, "resetTime");
            if (apiKeyAttResetTime!=null) {
                long resetTime = Long.valueOf(apiKeyAttResetTime);
                if (resetTime>System.currentTimeMillis()) {
                    // reset updateInfo's reset time to this stored value so we don't delay next update to a default amount
                    updateInfo.setResetTime("fitbit", resetTime);
                    throw new RateLimitReachedException();
                }
            }
        }
        if (remainingAPICalls!=null&&remainingAPICalls<1)
            throw new RateLimitReachedException();

        try {
            long then = System.currentTimeMillis();
            URL url = new URL(urlString);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            if (method!=null && method.length>0)
                request.setRequestMethod(method[0]);

            OAuthConsumer consumer = new DefaultOAuthConsumer(
                    getConsumerKey(updateInfo.apiKey), getConsumerSecret(updateInfo.apiKey));

            consumer.setTokenWithSecret(
                    guestService.getApiKeyAttribute(updateInfo.apiKey,"accessToken"),
                    guestService.getApiKeyAttribute(updateInfo.apiKey,"tokenSecret"));

            // sign the request (consumer is a Signpost DefaultOAuthConsumer)
            try {
                consumer.sign(request);
            } catch (Exception e) {
                throw new RuntimeException("OAuth exception: " + e.getMessage());
            }
            request.connect();
            final int httpResponseCode = request.getResponseCode();

            final String httpResponseMessage = request.getResponseMessage();

            // retrieve and save rate limiting metadata
            final String remainingCalls = request.getHeaderField("Fitbit-Rate-Limit-Remaining");
            if (remainingCalls!=null) {
                updateInfo.setRemainingAPICalls("fitbit", Integer.valueOf(remainingCalls));
                guestService.setApiKeyAttribute(updateInfo.apiKey, "remainingCalls", remainingCalls);
                if (Integer.valueOf(remainingCalls)==0)
                    setResetTime(updateInfo, request);
            }

            if (httpResponseCode == 200 || httpResponseCode == 201) {
                String json = IOUtils.toString(request.getInputStream());
                connectorUpdateService.addApiUpdate(updateInfo.apiKey,
                        objectTypes, then, System.currentTimeMillis() - then,
                        urlString, true, httpResponseCode, httpResponseMessage);
                // logger.info(updateInfo.apiKey.getGuestId(), "REST call success: " +
                // urlString);
                return json;
            } else {
                connectorUpdateService.addApiUpdate(updateInfo.apiKey,
                        objectTypes, then, System.currentTimeMillis() - then,
                        urlString, false, httpResponseCode, httpResponseMessage);
                // Check for response code 429 which is Fitbit's over rate limit error
                if(httpResponseCode == 429) {
                    logger.warn("Darn, we hit Fitbit's rate limit again! url=" + urlString + ", guest=" + updateInfo.getGuestId());
                    // try to retrieve the reset time from Fitbit, otherwise default to a one hour delay
                    // also, set resetTime as an apiKey attribute so we don't retry calling the API too soon
                    setResetTime(updateInfo, request);
                    throw new RateLimitReachedException();
                }
                else {
                    if (httpResponseCode == 409) {
                        // this is to account for this method being called when adding an api Subscription
                        throw new UnexpectedResponseCodeException(httpResponseCode, httpResponseMessage, urlString);
                    } else
                    // Otherwise throw the same error that SignpostOAuthHelper used to throw
                    if (httpResponseCode == 401)
                        throw new AuthExpiredException();
                    else if (httpResponseCode >= 400 && httpResponseCode < 500)
                        throw new UpdateFailedException("Unexpected response code: " + httpResponseCode, true,
                                ApiKey.PermanentFailReason.clientError(httpResponseCode));
                    throw new UpdateFailedException(false, "Error: " + httpResponseCode);
                }
            }
        } catch (IOException exc) {
            throw new RuntimeException("IOException trying to make rest call: " + exc.getMessage());
        }
    }

    private void setResetTime(UpdateInfo updateInfo, HttpURLConnection request) {
        final String rateLimitResetSeconds = request.getHeaderField("Fitbit-Rate-Limit-Reset");
        if (rateLimitResetSeconds!=null) {
            int millisUntilReset = Integer.valueOf(rateLimitResetSeconds)*1000;
            final long resetTime = System.currentTimeMillis() + millisUntilReset;
            guestService.setApiKeyAttribute(updateInfo.apiKey, "resetTime", String.valueOf(resetTime));
            updateInfo.setResetTime("fitbit", resetTime);
        } else {
            final long resetTime = System.currentTimeMillis() + 60 * DateTimeConstants.MILLIS_PER_HOUR;
            guestService.setApiKeyAttribute(updateInfo.apiKey, "resetTime", String.valueOf(resetTime));
            updateInfo.setResetTime("fitbit", resetTime);
        }
    }

    private String getConsumerSecret(ApiKey apiKey) {
        String consumerSecret = guestService.getApiKeyAttribute(apiKey, apiKey.getConnector().getName() + "ConsumerSecret");
        return consumerSecret == null ? "" : consumerSecret;
    }

    private String getConsumerKey(ApiKey apiKey) {
        String consumerKey = guestService.getApiKeyAttribute(apiKey, apiKey.getConnector().getName() + "ConsumerKey");
        return consumerKey == null ? "" : consumerKey;
    }

}
