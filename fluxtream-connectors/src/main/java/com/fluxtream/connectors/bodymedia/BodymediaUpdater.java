package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.Autonomous;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.TimeUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
@Updater(prettyName = "BodyMedia", value = 88,
         objectTypes = {BodymediaBurnFacet.class, BodymediaSleepFacet.class, BodymediaStepsFacet.class},
         hasFacets = true,
         defaultChannels = {"BodyMedia.mets", "BodyMedia.lying"})
public class BodymediaUpdater extends AbstractUpdater implements Autonomous {

    // TODO: make this configurable
    private static final int RATE_DELAY = 500;
    static FlxLogger logger = FlxLogger.getLogger(AbstractUpdater.class);

    @Autowired
    SignpostOAuthHelper signpostHelper;

    @Qualifier("metadataServiceImpl")
    @Autowired
    MetadataService metadataService;

    @Autowired
    BodymediaController bodymediaController;

    private final HashMap<ObjectType, String> url = new HashMap<ObjectType, String>();
    private final HashMap<ObjectType, Integer> maxIncrement = new HashMap<ObjectType, Integer>();

    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
    DateTimeFormatter form = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");
    private final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    protected static DateTimeFormatter tzmapFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");

    private Long timeOfLastCall;

    public BodymediaUpdater() {
        super();
        ObjectType burn = ObjectType.getObjectType(connector(), "burn");
        ObjectType sleep = ObjectType.getObjectType(connector(), "sleep");
        ObjectType steps = ObjectType.getObjectType(connector(), "steps");
        url.put(burn, "burn/day/minute/intensity/");
        url.put(sleep, "sleep/day/period/");
        url.put(steps, "step/day/hour/");
        maxIncrement.put(burn, 1);
        maxIncrement.put(sleep, 1);
        maxIncrement.put(steps, 31);
    }

    public void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        if (guestService.getApiKey(updateInfo.apiKey.getId())==null) {
            logger.info("Not updating BodyMedia connector instance with a deleted apiKeyId");
            return;
        }
        // There's no difference between the initial history update and the incremental updates, so
        // just call updateConnectorData in either case
        updateConnectorData(updateInfo);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        if (guestService.getApiKey(updateInfo.apiKey.getId())==null) {
            logger.info("Not updating BodyMedia connector instance with a deleted apiKeyId");
            return;
        }
        // Check if the token has expired, and if so try to exchange it
        checkAndReplaceOauthToken(updateInfo);

        // Get timezone map for this user
        TimezoneMap tzMap = getTimezoneMap(updateInfo);

        // Insert tzMap into the updateInfo context so it's accessible to the extractors
        updateInfo.setContext("tzMap", tzMap);

        for (ObjectType ot : connector().objectTypes()) {
            // Get the start date either from user registration info or from the stored apiKeyAttributes.
            // Set the end date to be today + 1 to cover the case of people in timezones which are later
            // than the timezone of the server where it's already the next day
            DateTime start = getStartDate(updateInfo, ot);
            DateTime end = new DateTime().plusDays(1);
            retrieveHistory(updateInfo, ot, start, end);
        }
    }

    private void checkAndReplaceOauthToken(UpdateInfo updateInfo) throws OAuthExpectationFailedException,
                                                                         OAuthMessageSignerException,
                                                                         OAuthNotAuthorizedException,
                                                                         OAuthCommunicationException {
        String time = guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenExpiration");
        if(time==null || Long.parseLong(time) < System.currentTimeMillis()/1000)
            bodymediaController.replaceToken(updateInfo);
    }

    /**
     * Retrieves that history for the given facet from the start date to the end date. It peforms the api calls in reverse order
     * starting from the end date. This is so that the most recent information is retrieved first.
     * @param updateInfo The api's info
     * @param ot The ObjectType that represents the facet to be updated
     * @param start The earliest date for which the burn history is retrieved. This date is included in the update.
     * @param end The latest date for which the burn history is retrieved. This date is also included in the update.
     * @throws Exception If either storing the data fails or if the rate limit is reached on Bodymedia's api
     */
    private void retrieveHistory(UpdateInfo updateInfo, ObjectType ot, DateTime start, DateTime end) throws Exception {
        final String urlExtension = url.get(ot);
        final int increment = maxIncrement.get(ot);
        DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();
        DateTime current = start;
        try {
            //  Loop from start to end, incrementing by the max number of days you can
            //  specify for a given type of query.  This is 1 for burn and sleep, and 31 for steps.
            //@ loop_invariant date.compareTo(userRegistrationDate) >= 0;
            while (comparator.compare(current, end) < 0)
            {
                if (guestService.getApiKey(updateInfo.apiKey.getId())==null) {
                    logger.info("Not updating BodyMedia connector instance with a deleted apiKeyId");
                    return;
                }
                String startPeriod = current.toString(formatter);
                String endPeriod = current.plusDays(increment - 1).toString(formatter);
                String minutesUrl = "http://api.bodymedia.com/v2/json/" + urlExtension + startPeriod + "/" + endPeriod +
                                    "?api_key=" + guestService.getApiKeyAttribute(updateInfo.apiKey,"bodymediaConsumerKey");
                //The following call may fail due to bodymedia's api. That is expected behavior
                enforceRateLimits();
                String json = signpostHelper.makeRestCall(updateInfo.apiKey, ot.value(), minutesUrl);
                guestService.setApiKeyAttribute(updateInfo.apiKey, "timeOfLastCall", String.valueOf(System.currentTimeMillis()));
                JSONObject bodymediaResponse = JSONObject.fromObject(json);
                JSONArray daysArray = bodymediaResponse.getJSONArray("days");
                if(bodymediaResponse.has("lastSync"))
                {
                    DateTime d = form.parseDateTime(bodymediaResponse.getJSONObject("lastSync").getString("dateTime"));

                    // Get timezone map from UpdateInfo context
                    TimezoneMap tzMap = (TimezoneMap)updateInfo.getContext("tzMap");

                    // Insert lastSync into the updateInfo context so it's accessible to the updater
                    updateInfo.setContext("lastSync", d);
                    List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
                    for(Object o : daysArray)
                    {
                        if(o instanceof JSONObject)
                        {
                            if (ot==ObjectType.getObjectType(connector(), "steps"))
                                newFacets.add(createOrUpdateStepsFacet((JSONObject)o, updateInfo, d, tzMap));
                            else if (ot==ObjectType.getObjectType(connector(), "burn"))
                                newFacets.add(createOrUpdateBurnFacet((JSONObject)o, updateInfo, d, tzMap));
                            else
                                newFacets.add(createOrUpdateSleepFacet((JSONObject)o, updateInfo, d, tzMap));
                        }
                    }
                    bodyTrackStorageService.storeApiData(updateInfo.getGuestId(), newFacets);
                }

                current = current.plusDays(increment);
            }

            // Update the stored value that controls when we will start updating next time
            updateStartDate(updateInfo,ot,current);
        }
        catch (Exception e) {
            StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=BodymediaUpdater.retrieveHistory")
                                                .append(" message=\"exception while retrieving history\" connector=")
                                                .append(updateInfo.apiKey.getConnector().toString())
                                                .append(" guestId=").append(updateInfo.apiKey.getGuestId())
                                                .append(" updatingDate=").append(current);
            logger.info(sb.toString());

            // Update the stored value that controls when we will start updating next time
            updateStartDate(updateInfo,ot,current);

            // Rethrow the error so that this task gets rescheduled
            throw e;
        }

    }

    /**
     * make sure that there is at least RATE_DELAY ms between each API call. This works
     * because Updaters are singletons
     */
    private void enforceRateLimits() {
        long waitTime = getWaitTime();
        if (waitTime>0) {
            try { Thread.currentThread().sleep(waitTime); }
            catch(Throwable e) {
                e.printStackTrace();
                throw new RuntimeException("Unexpected error waiting to enforce rate limits.");
            }
        }
        setCallTime();
    }

    private long getWaitTime() {
        final long millisSinceLastCall = getMillisSinceLastCall();
        if (millisSinceLastCall ==-1)
            return -1;
        else return RATE_DELAY-millisSinceLastCall;
    }

    /**
     * Thread-safely return the time since we last made an API call
     * @return last time of API call
     */
    private synchronized long getMillisSinceLastCall() {
        if (timeOfLastCall==null)
            return -1;
        return System.currentTimeMillis()-timeOfLastCall;
    }

    /**
     * Thread-safely save the time; this called each time we make an API call
     */
    private synchronized void setCallTime() {
        timeOfLastCall = System.currentTimeMillis();
    }

    private BodymediaSleepFacet createOrUpdateSleepFacet(final JSONObject day, final UpdateInfo updateInfo, final DateTime d, final TimezoneMap tzMap) {
        final DateTime date = formatter.parseDateTime(day.getString("date"));
        final String dateString = dateFormatter.print(date.getMillis());
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.date=?",
                                                                                   updateInfo.apiKey.getId(), dateString);
        final ApiDataService.FacetModifier<BodymediaSleepFacet> facetModifier = new ApiDataService.FacetModifier<BodymediaSleepFacet>() {
            @Override
            public BodymediaSleepFacet createOrModify(BodymediaSleepFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new BodymediaSleepFacet(updateInfo.apiKey.getId());
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                    facet.timeUpdated = System.currentTimeMillis();
                }
                facet.efficiency = day.getDouble("efficiency");
                facet.totalLying = day.getInt("totalLying");
                facet.totalSleeping = day.getInt("totalSleep");
                facet.json = day.getString("sleepPeriods");
                facet.lastSync = d.getMillis();
                facet.date = dateString;

                //https://developer.bodymedia.com/docs/read/api_reference_v2/Sleep_Service
                //  sleep data is from noon the previous day to noon the current day,
                //  so subtract MILLIS_IN_DAY/2 from midnight

                long MILLIS_IN_DAY = 86400000l;

                if(tzMap!=null)
                {
                    // Create a LocalDate object which just captures the date without any
                    // timezone assumptions
                    LocalDate ld = new LocalDate(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
                    // Use tzMap to convert date into a datetime with timezone information
                    DateTime realDateStart = tzMap.getStartOfDate(ld);
                    // Set the start and end times for the facet.  The start time is the leading midnight
                    // of burn.date according to BodyMedia's idea of what timezone you were in then.
                    // End should, I think, be start + the number of minutes in the minutes array *
                    // the number of milliseconds in a minute.
                    facet.date = dateFormatter.print(realDateStart.getMillis());
                    facet.start = realDateStart.getMillis() - DateTimeConstants.MILLIS_PER_DAY/2;
                    facet.end = realDateStart.getMillis() + DateTimeConstants.MILLIS_PER_DAY/2;
                }
                else {
                    facet.date = dateFormatter.print(date.getMillis());
                    TimeZone timeZone = metadataService.getTimeZone(updateInfo.getGuestId(), date.getMillis());
                    long fromNoon = TimeUtils.fromMidnight(date.getMillis(), timeZone) - MILLIS_IN_DAY / 2;
                    long toNoon = TimeUtils.toMidnight(date.getMillis(), timeZone) - MILLIS_IN_DAY / 2;
                    facet.start = fromNoon;
                    facet.end = toNoon;
                }
                return facet;
            }
        };
        return apiDataService.createOrReadModifyWrite(BodymediaSleepFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private BodymediaBurnFacet createOrUpdateBurnFacet(final JSONObject day, final UpdateInfo updateInfo, final DateTime d, final TimezoneMap tzMap) {
        final DateTime date = formatter.parseDateTime(day.getString("date"));
        final String dateString = dateFormatter.print(date.getMillis());
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.date=?",
                                                                                   updateInfo.apiKey.getId(), dateString);
        final ApiDataService.FacetModifier<BodymediaBurnFacet> facetModifier = new ApiDataService.FacetModifier<BodymediaBurnFacet>() {
            @Override
            public BodymediaBurnFacet createOrModify(BodymediaBurnFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new BodymediaBurnFacet(updateInfo.apiKey.getId());
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                    facet.timeUpdated = System.currentTimeMillis();
                }
                facet.setTotalCalories(day.getInt("totalCalories"));
                facet.setEstimatedCalories(day.getInt("estimatedCalories"));
                facet.setPredictedCalories(day.getInt("predictedCalories"));
                facet.json = day.getString("minutes");
                facet.lastSync = d.getMillis();

                facet.date = dateString;

                if(tzMap!=null)
                {
                    // Create a LocalDate object which just captures the date without any
                    // timezone assumptions
                    LocalDate ld = new LocalDate(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
                    // Use tzMap to convert date into a datetime with timezone information
                    DateTime realDateStart = tzMap.getStartOfDate(ld);
                    // Set the start and end times for the facet.  The start time is the leading midnight
                    // of burn.date according to BodyMedia's idea of what timezone you were in then.
                    // End should, I think, be start + the number of minutes in the minutes array *
                    // the number of milliseconds in a minute.
                    facet.start = realDateStart.getMillis();
                    int minutesLength = 1440;
                    facet.end = facet.start + DateTimeConstants.MILLIS_PER_MINUTE * minutesLength;
                }
                else {
                    // This is the old code from Prasanth that uses metadataService, which isn't right
                    TimeZone timeZone = metadataService.getTimeZone(updateInfo.getGuestId(), date.getMillis());
                    long fromMidnight = TimeUtils.fromMidnight(date.getMillis(), timeZone);
                    long toMidnight = TimeUtils.toMidnight(date.getMillis(), timeZone);
                    //Sets the start and end times for the facet so that it can be uniquely defined
                    facet.start = fromMidnight;
                    facet.end = toMidnight;
                }
                return facet;
            }
        };
        return apiDataService.createOrReadModifyWrite(BodymediaBurnFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    private BodymediaStepsFacet createOrUpdateStepsFacet(final JSONObject day, final UpdateInfo updateInfo, final DateTime d, final TimezoneMap tzMap) {
        final DateTime date = formatter.parseDateTime(day.getString("date"));
        final String dateString = dateFormatter.print(date.getMillis());
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.date=?",
                                                                                   updateInfo.apiKey.getId(), dateString);
        final ApiDataService.FacetModifier<BodymediaStepsFacet> facetModifier = new ApiDataService.FacetModifier<BodymediaStepsFacet>() {
            @Override
            public BodymediaStepsFacet createOrModify(BodymediaStepsFacet facet, final Long apiKeyId) {
                if (facet == null) {
                    facet = new BodymediaStepsFacet(updateInfo.apiKey.getId());
                    facet.guestId = updateInfo.apiKey.getGuestId();
                    facet.api = updateInfo.apiKey.getConnector().value();
                    facet.timeUpdated = System.currentTimeMillis();
                }
                facet.totalSteps = day.getInt("totalSteps");
                facet.json = day.getString("hours");
                facet.lastSync = d.getMillis();

                facet.date = dateString;
                if(tzMap!=null)
                {
                    // Create a LocalDate object which just captures the date without any
                    // timezone assumptions
                    LocalDate ld = new LocalDate(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
                    // Use tzMap to convert date into a datetime with timezone information
                    DateTime realDateStart = tzMap.getStartOfDate(ld);
                    // Set the start and end times for the facet.  The start time is the leading midnight
                    // of date according to BodyMedia's idea of what timezone you were in then.
                    // Need to figure out what end should be...
                    facet.start = realDateStart.getMillis();
                    int minutesLength = 1440;
                    facet.end = facet.start + DateTimeConstants.MILLIS_PER_MINUTE * minutesLength;
                }
                else {
                    TimeZone timeZone = metadataService.getTimeZone(updateInfo.getGuestId(), date.getMillis());
                    long fromMidnight = TimeUtils.fromMidnight(date.getMillis(), timeZone);
                    long toMidnight = TimeUtils.toMidnight(date.getMillis(), timeZone);
                    facet.start = fromMidnight;
                    facet.end = toMidnight;
                }
                return facet;
            }
        };
        return apiDataService.createOrReadModifyWrite(BodymediaStepsFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    // Update the start date for next time.  The parameter the updateProgressDate is the date
    // of that retrieveHistory had gotten to when it completed or gave up.
    // If lastSync is set and is < updateProgressDate we will use that, and otherwise use updateProgressDate.
    void updateStartDate(UpdateInfo updateInfo, ObjectType ot, DateTime updateProgressTime)
    {
        DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

        // Calculate the name of the key in the ApiAttributes table
        // where the next start of update for this object type is
        // stored and retrieve the stored value.  This stored value
        // may potentially be null if something happened to the attributes table
        String updateKeyName = "BodyMedia." + ot.getName() + ".updateStartDate";
        String storedUpdateStartDate = guestService.getApiKeyAttribute(updateInfo.apiKey, updateKeyName);

        // Retrieve the lastSync date if it has been added to the
        // updateInfo context by an extractor
        DateTime lastSync = (DateTime)updateInfo.getContext("lastSync");


        // Check which is earlier: the lastSync time returned from Bodymedia or the
        // point we were in the update that just ended.  Store the lesser of the two
        // in nextUpdateStartDate
        String nextUpdateStartDate = storedUpdateStartDate;
        if (lastSync != null) {
            if (comparator.compare(updateProgressTime, lastSync) > 0) {
                // lastSync from Bodymedia is less than the update progress
                nextUpdateStartDate = lastSync.toString(formatter);
            }
            else {
                // the update progress is less than lastSync from Bodymedia
                nextUpdateStartDate = updateProgressTime.toString(formatter);
            }
        }
        else {
            // Last sync is null, just leave the stored updateTime
            // alone since it's better to get some extra data next
            // time than to skip data from dates that potentially changed
        }

        // Store the new value if it's different than what's stored in ApiKeyAttributes
        if(storedUpdateStartDate==null || !nextUpdateStartDate.equals(storedUpdateStartDate)) {
            guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, nextUpdateStartDate);
        }
    }

    OAuthConsumer setupConsumer(ApiKey apiKey) {
        String api_key = guestService.getApiKeyAttribute(apiKey, "bodymediaConsumerKey");
        String bodymediaConsumerSecret = guestService.getApiKeyAttribute(apiKey, "bodymediaConsumerSecret");

        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(api_key, bodymediaConsumerSecret);

        String accessToken = guestService.getApiKeyAttribute(apiKey,"accessToken");
        String tokenSecret = guestService.getApiKeyAttribute(apiKey,"tokenSecret");

        consumer.setTokenWithSecret(accessToken, tokenSecret);
        return consumer;
    }

    public DateTime getStartDate(UpdateInfo updateInfo, ObjectType ot) throws Exception
    {
        ApiKey apiKey = updateInfo.apiKey;

        // The updateStartDate for a given object type is stored in the apiKeyAttributes
        // as BodyMedia.<objectName>.updateStartDate.  In the case of a failure the updater will store the date
        // that failed and start there next time.  In the case of a successfully completed update it will store
        // the lastSync date returned from BodyMedia along with each API call.
        String updateKeyName = "BodyMedia." + ot.getName() + ".updateStartDate";
        String updateStartDate = guestService.getApiKeyAttribute(apiKey, updateKeyName);

        // The first time we do this there won't be an apiKeyAttribute yet.  In that case get the
        // registration date for the user and store that.

        if(updateStartDate == null) {
            OAuthConsumer consumer = setupConsumer(updateInfo.apiKey);
            String api_key = guestService.getApiKeyAttribute(updateInfo.apiKey, "bodymediaConsumerKey");
            updateStartDate = getUserRegistrationDate(updateInfo, api_key, consumer);

            // This is a hack to deal with backward compatibility with systems containing data
            // from earlier versions of this updater.  The algorighm for setting start and end times
            // was flawed in earlier versions.  It used the inferred timezone from the metadata service
            // rather than the timezone map from BodyMedia.  This means that the usual apiUpdateService
            // method for dealing with duplicate facets won't work since it just matches on the basis
            // of start and end times.

            // When we are at this point in the code, this connector is either:
            //   1) just created after the new version of the connector too effect, or
            //   2) a legacy connector which has stale data lying around
            // In the first case, it does no harm to delete all the facets in the objects facet
            // table at this point, since we're about to import all the data starting from the
            // registration date.  In the second case, we need to delete the existing data or we'll
            // potentially end up with duplicates.
            String sqlTableName=JPAUtils.getEntityName(ot.facetClass());

            System.out.println("***** Detected first run of new BodymediaUpdater for guestId=" +
                               updateInfo.getGuestId() + " objectType=" + ot.getName() + " apiKeyId="+ updateInfo.apiKey.getId());

            jpaDaoService.execute("DELETE FROM " + sqlTableName + " WHERE apiKeyId=" + updateInfo.apiKey.getId());

            // Store in the apiKeyAttribute for next time
            guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, updateStartDate);
        }
        try{
            return formatter.parseDateTime(updateStartDate);
        } catch (IllegalArgumentException e){
            return dateFormatter.parseDateTime(updateStartDate);
        }
    }

    public String getUserRegistrationDate(UpdateInfo updateInfo, String api_key, OAuthConsumer consumer) throws Exception {
        // dev only: artificially make history shorter
        //return "20130810";
        long then = System.currentTimeMillis();
        String requestUrl = "http://api.bodymedia.com/v2/json/user/info?api_key=" + api_key;

        HttpGet request = new HttpGet(requestUrl);
        consumer.sign(request);
        HttpClient client = env.getHttpClient();
        enforceRateLimits();
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        final String reasonPhrase = response.getStatusLine().getReasonPhrase();
        if (statusCode == 200) {
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String json = responseHandler.handleResponse(response);
            JSONObject userInfo = JSONObject.fromObject(json);
            return userInfo.getString("registrationDate");
        }
        else {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl, reasonPhrase, statusCode, reasonPhrase);
            throw new Exception("Error: " + statusCode + " Unexpected error trying to get BodyMedia user registration date for guestId="+updateInfo.getGuestId());
        }
    }

    public TimezoneMap getTimezoneMap(UpdateInfo updateInfo) throws Exception {
            OAuthConsumer consumer = setupConsumer(updateInfo.apiKey);
            String api_key = guestService.getApiKeyAttribute(updateInfo.apiKey, "bodymediaConsumerKey");
            JSONArray timezoneMapJson = getUserTimezoneHistory(updateInfo, api_key, consumer);
            TimezoneMap ret= new TimezoneMap();

            try{
                for(int i=0; i<timezoneMapJson.size(); i++) {
                    JSONObject jsonRecord = timezoneMapJson.getJSONObject(i);
                    final String tzName = jsonRecord.getString("value");
                    final String startDateStr = jsonRecord.getString("startDate");
                    final String endDateStr = jsonRecord.optString("endDate");
                    DateTime startDate;
                    DateTime endDate;
                    DateTimeZone tz;

                    tz = DateTimeZone.forID(tzName);
                    startDate = tzmapFormatter.parseDateTime(startDateStr);
                    if(endDateStr.equals("")) {
                        // Last entry in table has no endDate, set it to be one day in the future
                        endDate=new DateTime(tz).plusDays(1);
                    }
                    else {
                        endDate = tzmapFormatter.parseDateTime(endDateStr);
                    }

                    ret.add(startDate.getMillis(), endDate.getMillis(),tz);
                }

            } catch (Throwable e){
                StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=BodymediaUpdater.getTimezoneMap")
                                    .append(" message=\"exception while getting timezone map\" connector=")
                                    .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                                    .append(updateInfo.apiKey.getGuestId())
                                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");;
                logger.info(sb.toString());
            }
            return ret;
        }

    public JSONArray getUserTimezoneHistory(UpdateInfo updateInfo, String api_key, OAuthConsumer consumer) throws Exception {
        long then = System.currentTimeMillis();
        String requestUrl = "http://api.bodymedia.com/v2/json/timezone?api_key=" + api_key;

        HttpGet request = new HttpGet(requestUrl);
        consumer.sign(request);
        HttpClient client = env.getHttpClient();
        enforceRateLimits();
        HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        final String reasonPhrase = response.getStatusLine().getReasonPhrase();
        if (statusCode == 200) {
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String json = responseHandler.handleResponse(response);
            JSONObject userInfo = JSONObject.fromObject(json);
            return userInfo.getJSONArray("timezones");
        }
        else {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl, "", statusCode, reasonPhrase);
            throw new Exception("Error: " + statusCode + " Unexpected error trying to bodymedia timezone for user " + updateInfo.apiKey.getGuestId());
        }
    }
}

