package com.fluxtream.connectors.bodymedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
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
public class BodymediaUpdater extends AbstractUpdater {
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
    private final DateTimeFormatter formatter2 = DateTimeFormat.forPattern("yyyy-MM-dd");

    protected static DateTimeFormatter tzmapFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");

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
        // There's no difference between the initial history update and the incremental updates, so
        // just call updateConnectorData in either case
        updateConnectorData(updateInfo);
    }

    private void checkAndReplaceOauthToken(UpdateInfo updateInfo) throws OAuthExpectationFailedException,
                                                                         OAuthMessageSignerException,
                                                                         OAuthNotAuthorizedException,
                                                                         OAuthCommunicationException {
        String time = guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenExpiration");
        if(Long.parseLong(time) < System.currentTimeMillis()/1000)
            bodymediaController.replaceToken(updateInfo);
    }

    /**
     * Retrieves that history for the given facet from the start date to the end date. It peforms the api calls in reverse order
     * starting from the end date. This is so that the most recent information is retrieved first.
     * @param updateInfo The api's info
     * @param ot The ObjectType that represents the facet to be updated
     * @param urlExtension the request uri used for the api
     * @param increment the number of days to retrieve at once from bodymedia
     * @param start The earliest date for which the burn history is retrieved. This date is included in the update.
     * @param end The latest date for which the burn history is retrieved. This date is also included in the update.
     * @throws Exception If either storing the data fails or if the rate limit is reached on Bodymedia's api
     */
    private void retrieveHistory(UpdateInfo updateInfo, ObjectType ot, String urlExtension, int increment, DateTime start, DateTime end) throws Exception {
        DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();
        DateTime current = start;
        try {
            //  Loop from start to end, incrementing by the max number of days you can
            //  specify for a given type of query.  This is 1 for burn and sleep, and 31 for steps.
            //@ loop_invariant date.compareTo(userRegistrationDate) >= 0;
            while (comparator.compare(current, end) < 0)
            {
                String startPeriod = current.toString(formatter);
                String endPeriod = current.plusDays(increment - 1).toString(formatter);
                String minutesUrl = "http://api.bodymedia.com/v2/json/" + urlExtension + startPeriod + "/" + endPeriod +
                                    "?api_key=" + updateInfo.apiKey.getAttributeValue("api_key", env);
                //The following call may fail due to bodymedia's api. That is expected behavior
                String jsonResponse = signpostHelper.makeRestCall(updateInfo.apiKey, ot.value(), minutesUrl);
                apiDataService.cacheApiDataJSON(updateInfo, jsonResponse, -1, -1);
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

    // Update the start date for next time.  The parameter the updateProgressDate is the date
    // of that retrieveHistory had gotten to when it completed or gave up.
    // If lastSync is set and is < updateProgressDate we will use that, and otherwise use updateProgressDate.
    void updateStartDate(UpdateInfo updateInfo, ObjectType ot, DateTime updateProgressTime)
    {
        DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();

        // Retrieve the lastSync date if set in an extractor
        DateTime lastSync = (DateTime)updateInfo.getContext("lastSync");
        String updateProgressDate = updateProgressTime.toString(formatter);

        // Default updateStartDate to be
        String nextUpdateStartDate = updateProgressDate;
        if (lastSync != null) {
            if (comparator.compare(updateProgressTime, lastSync) > 0) {
                nextUpdateStartDate = lastSync.toString(formatter);
            }
        }

        String updateKeyName = "BodyMedia." + ot.getName() + ".updateStartDate";
        guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, nextUpdateStartDate);
    }

    OAuthConsumer setupConsumer(ApiKey apiKey) {
        String api_key = env.get("bodymediaConsumerKey");
        String bodymediaConsumerSecret = env.get("bodymediaConsumerSecret");

        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(api_key, bodymediaConsumerSecret);

        String accessToken = apiKey.getAttributeValue("accessToken", env);
        String tokenSecret = apiKey.getAttributeValue("tokenSecret", env);

        consumer.setTokenWithSecret(accessToken, tokenSecret);
        return consumer;
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        // Get timezone map for this user
        TimezoneMap tzMap = getTimezoneMap(updateInfo);

        // Insert tzMap into the updateInfo context so it's accessible to the extractors
        updateInfo.setContext("tzMap", tzMap);

        //checkAndReplaceOauthToken(updateInfo);
        for (ObjectType ot : updateInfo.objectTypes()) {
            // Get the start date either from user registration info or from the stored apiKeyAttributes.
            // Set the end date to be today + 1 to cover the case of people in timezones which are later
            // than the timezone of the server where it's already the next day
            DateTime start = getStartDate(updateInfo, ot);
            DateTime end = new DateTime().plusDays(1);
            retrieveHistory(updateInfo, ot, url.get(ot), maxIncrement.get(ot), start, end);
        }
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
            String api_key = env.get("bodymediaConsumerKey");
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
            return formatter2.parseDateTime(updateStartDate);
        }
    }

    public String getUserRegistrationDate(UpdateInfo updateInfo, String api_key, OAuthConsumer consumer) throws Exception {
        long then = System.currentTimeMillis();
        String requestUrl = "http://api.bodymedia.com/v2/json/user/info?api_key=" + api_key;

        HttpGet request = new HttpGet(requestUrl);
        consumer.sign(request);
        HttpClient client = env.getHttpClient();
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String json = responseHandler.handleResponse(response);
            JSONObject userInfo = JSONObject.fromObject(json);
            return userInfo.getString("registrationDate");
        }
        else {
            final String reasonPhrase = response.getStatusLine().getReasonPhrase();
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl, reasonPhrase);
            throw new Exception("Error: " + statusCode + " Unexpected error trying to get BodyMedia user registration date for guestId="+updateInfo.getGuestId());
        }
    }

    public TimezoneMap getTimezoneMap(UpdateInfo updateInfo) throws Exception {
            OAuthConsumer consumer = setupConsumer(updateInfo.apiKey);
            String api_key = env.get("bodymediaConsumerKey");
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
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String json = responseHandler.handleResponse(response);
            JSONObject userInfo = JSONObject.fromObject(json);
            return userInfo.getJSONArray("timezones");
        }
        else {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, requestUrl, "");
            throw new Exception("Error: " + statusCode + " Unexpected error trying to bodymedia timezone for user " + updateInfo.apiKey.getGuestId());
        }
    }
}

