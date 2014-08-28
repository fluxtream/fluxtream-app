package org.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.ExceptionUtils;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.AuthRevokedException;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
@Updater(prettyName = "RunKeeper", value = 35, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {LocationFacet.class, RunKeeperFitnessActivityFacet.class}, defaultChannels = {"runkeeper.totalCalories"})
public class RunKeeperUpdater  extends AbstractUpdater {

    Logger logger = Logger.getLogger(RunKeeperUpdater.class);

    final String DEFAULT_ENDPOINT= "https://api.runkeeper.com";

    @Autowired
    RunKeeperController runKeeperController;

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    BodyTrackHelper bodytrackHelper;

    final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

    final String barsStyle = "{\"styles\":[{\"type\":\"line\",\"show\":false,\"lineWidth\":1}," +
                                     "{\"radius\":0,\"fill\":false,\"type\":\"lollipop\",\"show\":true,\"lineWidth\":4}," +
                                     "{\"radius\":2,\"fill\":true,\"type\":\"point\",\"show\":false,\"lineWidth\":1}," +
                                     "{\"marginWidth\":5,\"verticalOffset\":7," +
                                     "\"numberFormat\":\"###,##0\",\"type\":\"value\",\"show\":false}]," +
                                     "\"comments\":" +
                                     "{\"styles\":[{\"radius\":3,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}]," +
                                     "\"verticalMargin\":4,\"show\":true}}";
    @Autowired
    MetadataService metadataService;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        updateData(updateInfo, 0);
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        final String entityName = JPAUtils.getEntityName(RunKeeperFitnessActivityFacet.class);
        final List<RunKeeperFitnessActivityFacet> newest = jpaDaoService.executeQueryWithLimit(
                "SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC",
                1,
                RunKeeperFitnessActivityFacet.class, updateInfo.apiKey.getId());
        // If there are existing runkeeper facets, start just after the end of the most recent one.
        // If there are no existing runkeeper facets, start at 0 just like we would for an
        // initial history update.
        long lastUpdated = 0;
        if (newest.size()>0) {
            lastUpdated = newest.get(0).end;
            System.out.println("Runkeeper: starting update from " + timeFormatter.print(lastUpdated) + ", guestId=" + updateInfo.getGuestId());
        }
        else {
            System.out.println("Runkeeper has no existing facets.  Starting update from time=0, guestId=" + updateInfo.getGuestId());
        }
        updateData(updateInfo, lastUpdated);
    }

    private void updateData(final UpdateInfo updateInfo, final long since) throws Exception {
        // Set the channel defaults for the Runkeeper datastore channels.  It is a bit of a hack
        // to do this here, but it's convenient to do so since we know that this function is
        // going to be run for both existing and new connectors.  Set most of the channels to
        // default to show as bars rather than lines
        bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "runkeeper", "minutesPerKilometer", barsStyle);
        bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "runkeeper", "minutesPerMile", barsStyle);
        bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "runkeeper", "totalCalories", barsStyle);

        String url = DEFAULT_ENDPOINT+"/user?oauth_token=";
        final String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
        final Token token = new Token(accessToken, guestService.getApiKeyAttribute(updateInfo.apiKey, "runkeeperConsumerSecret"));
        final String userEndpoint = url + accessToken;
        OAuthRequest request = new OAuthRequest(Verb.GET, userEndpoint);
        request.addHeader("Accept", "application/vnd.com.runkeeper.User+json");
        final OAuthService service = runKeeperController.getOAuthService();
        service.signRequest(token, request);
        Response response = request.send();
        final int httpResponseCode = response.getCode();
        long then = System.currentTimeMillis();
        String body = response.getBody();

        if (httpResponseCode==200) {
            JSONObject jsonObject = JSONObject.fromObject(body);
            String fitnessActivities = jsonObject.getString("fitness_activities");
            List<String> activities = new ArrayList<String>();
            String activityFeedURL = DEFAULT_ENDPOINT + fitnessActivities;
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, request.getCompleteUrl());

            getFitnessActivityFeed(updateInfo, service, token, activityFeedURL, 25, activities, since);
            Collections.reverse(activities);
            getFitnessActivities(updateInfo, service, token, activities);
        } else {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then,
                               request.getCompleteUrl(), ExceptionUtils.getStackTrace(new Exception()),
                               httpResponseCode, body);
            if (httpResponseCode==403) {
                handleTokenRevocation(body);
            }
            if (httpResponseCode>=400&&httpResponseCode<500)
                throw new UpdateFailedException("Unexpected response code: " + httpResponseCode, true,
                                                ApiKey.PermanentFailReason.clientError(httpResponseCode));
            else
                throw new UpdateFailedException("Unexpected code: " + httpResponseCode);
        }
    }

    private void handleTokenRevocation(final String responseBody) throws AuthRevokedException {
        // let's try to parse this error's payload and be conservative about parsing errors here
        boolean dataCleanupRequested = false;
        if (responseBody!=null) {
            try {
                final JSONObject errorPayload = JSONObject.fromObject(responseBody);
                if (errorPayload != null && errorPayload.has("reason")&&errorPayload.getString("reason").equalsIgnoreCase("Revoked")) {
                    if (errorPayload.has("delete_health")&&errorPayload.getBoolean("delete_health"))
                        dataCleanupRequested = true;
                    throw new AuthRevokedException(dataCleanupRequested);
                }
            } catch (AuthRevokedException t) {
                throw t;
            } catch (Throwable t) {
            }
        }
    }

    private void getFitnessActivities(final UpdateInfo updateInfo, final OAuthService service,
                                      final Token token, final List<String> activities) throws Exception {
        for (String activity : activities) {
            if (guestService.getApiKey(updateInfo.apiKey.getId())==null)
                break;
            String activityURL = DEFAULT_ENDPOINT + activity;
            OAuthRequest request = new OAuthRequest(Verb.GET, activityURL);
            request.addQuerystringParameter("oauth_token", token.getToken());
            request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivity+json");
            service.signRequest(token, request);
            long then = System.currentTimeMillis();
            Response response = request.send();
            final int httpResponseCode = response.getCode();
            String body = response.getBody();
            if (httpResponseCode ==200) {
                countSuccessfulApiCall(updateInfo.apiKey,
                                       updateInfo.objectTypes, then, activityURL);
                JSONObject jsonObject = JSONObject.fromObject(body);
                createOrUpdateActivity(jsonObject, updateInfo);
            } else {
                countFailedApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityURL, ExceptionUtils.getStackTrace(new Exception()),
                                   httpResponseCode, body);
                if (httpResponseCode==403)
                    handleTokenRevocation(body);
                if (httpResponseCode>=400&&httpResponseCode<500)
                    throw new UpdateFailedException("Unexpected response code: " + httpResponseCode, true, ApiKey.PermanentFailReason.clientError(httpResponseCode));
                else
                    throw new UpdateFailedException("Unexpected code: " + httpResponseCode);
            }
        }
    }

    private void createOrUpdateActivity(final JSONObject jsonObject, final UpdateInfo updateInfo) throws Exception {
        final String uri = jsonObject.getString("uri");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.uri=?",
                                                                                   updateInfo.apiKey.getId(), uri);
        final ApiDataService.FacetModifier<RunKeeperFitnessActivityFacet> facetModifier = new ApiDataService.FacetModifier<RunKeeperFitnessActivityFacet>() {
            @Override
            public RunKeeperFitnessActivityFacet createOrModify(RunKeeperFitnessActivityFacet origFacet, final Long apiKeyId) {
                try {
                    RunKeeperFitnessActivityFacet facet = origFacet;
                    if (facet==null) {
                        facet = new RunKeeperFitnessActivityFacet(updateInfo.apiKey.getId());
                        facet.uri = uri;
                        facet.api = updateInfo.apiKey.getConnector().value();
                        facet.guestId = updateInfo.apiKey.getGuestId();
                        facet.timeUpdated = System.currentTimeMillis();
                    }
                    boolean startTimeSet = false;
                    if (jsonObject.has("path")) {
                        final JSONArray path = jsonObject.getJSONArray("path");
                        List<LocationFacet> locationFacets = new ArrayList<LocationFacet>();
                        for (int i=0; i<path.size(); i++) {
                            JSONObject pathElement = path.getJSONObject(i);
                            LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());
                            locationFacet.latitude = (float) pathElement.getDouble("latitude");
                            locationFacet.longitude = (float) pathElement.getDouble("longitude");
                            if (!startTimeSet) {
                                // we need to know the user's location in order to figure out
                                // his timezone
                                final String start_time = jsonObject.getString("start_time");
                                System.out.println("runkeeper activity start time: " + start_time + " (should be ascending), guestId=" + updateInfo.getGuestId());
                                final TimeZone timeZone = metadataService.getTimeZone(locationFacet.latitude, locationFacet.longitude);
                                facet.start = timeFormatter.withZone(DateTimeZone.forTimeZone(timeZone)).parseMillis(start_time);
                                facet.timeZone = timeZone.getID();
                                final int duration = jsonObject.getInt("duration");
                                facet.end = facet.start + duration*1000;
                                facet.duration = duration;
                                startTimeSet = true;
                            }
                            locationFacet.altitude = (int) pathElement.getDouble("altitude");
                            final long millisIncrement = (long)(pathElement.getDouble("timestamp") * 1000d);
                            locationFacet.timestampMs = facet.start + millisIncrement;
                            locationFacet.start = locationFacet.timestampMs;
                            locationFacet.end = locationFacet.timestampMs;
                            locationFacet.source = LocationFacet.Source.RUNKEEPER;
                            locationFacet.apiKeyId = updateInfo.apiKey.getId();
                            locationFacet.api = Connector.getConnector("runkeeper").value();
                            locationFacet.uri = uri;

                            locationFacets.add(locationFacet);
                        }
                        apiDataService.addGuestLocations(updateInfo.getGuestId(), locationFacets);
                    } else {
                        //TODO: abort elegantly if we don't have gps data as we are unable to figure out time
                        //in this case
                        return null;
                    }

                    facet.userID = jsonObject.getString("userID");
                    facet.duration = jsonObject.getInt("duration");
                    facet.type = jsonObject.getString("type");
                    facet.equipment = jsonObject.getString("equipment");
                    facet.total_distance = jsonObject.getDouble("total_distance");
                    facet.is_live = jsonObject.getBoolean("is_live");
                    facet.comments = jsonObject.getString("comments");
                    facet.uri = uri;
                    if (jsonObject.has("total_climb"))
                        facet.total_climb = jsonObject.getDouble("total_climb");

                    if (jsonObject.has("heart_rate")) {
                        final JSONArray heartRateArray = jsonObject.getJSONArray("heart_rate");
                        double totalHeartRate = 0d;
                        double totalTime = 0d;
                        double lastTimestamp = 0d;
                        for (int i=0; i<heartRateArray.size(); i++) {
                            JSONObject record = heartRateArray.getJSONObject(i);
                            double timestamp = record.getDouble("timestamp");
                            final double lap = timestamp - lastTimestamp;
                            totalHeartRate += record.getInt("heart_rate") * lap;
                            lastTimestamp = timestamp;
                            totalTime += lap;
                        }
                        facet.averageHeartRate = (int) (totalHeartRate/totalTime);
                        facet.heartRateStorage = heartRateArray.toString();
                    }
                    if (jsonObject.has("calories")) {
                        final JSONArray caloriesArray = jsonObject.getJSONArray("calories");
                        for (int i=0; i<caloriesArray.size(); i++) {
                            JSONObject record = caloriesArray.getJSONObject(i);
                            facet.totalCalories += record.getDouble("calories");
                        }
                        facet.caloriesStorage = caloriesArray.toString();
                    }
                    if (jsonObject.has("total_calories"))
                        facet.totalCalories = jsonObject.getDouble("total_calories");
                    if (jsonObject.has("distance")) {
                        final JSONArray distanceArray = jsonObject.getJSONArray("distance");
                        facet.distanceStorage = distanceArray.toString();
                    }
                    return facet;
                } catch (Throwable t) {
                    logger.warn("could not import a Runkeeper Activity record: " + t.getMessage());
                    return origFacet;
                }
            }
        };
        final RunKeeperFitnessActivityFacet newFacet = apiDataService.createOrReadModifyWrite(RunKeeperFitnessActivityFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        if (newFacet!=null) {
            List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
            newFacets.add(newFacet);
            bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), newFacets);
        }
    }

    /**
     * Get the feed of activities in a succint format. FitnessActivity info (with gps data etc) is fetched in a separate call
     * (one per activity). We want to limit this feed to those activities that we haven't already stored of course but
     * unfortunately the Runkeeper API call will by default retrieve the entire feed. Optional parameters
     * (<code>noEarlierThan</code>, <code>noLaterThan</code>) are able to limit the dataset, but they will only accept dates specified in
     * <code>yyyy-MM-DD</code> format, which obviously limits the boundary limits granularity to a day. Additionally, it is unclear
     * what timezone is used to filter the dataset (is it GMT, that is then converted to the local time, or is the
     * parameter given in local time?). Consequently, we use the <code>noEarlierThan</code> parameter with a one day padding and
     * further filter the dataset using the list of activity that we already have data for (<code>activityIsAlreadyStored()</code>).
     * @param updateInfo
     * @param service
     * @param token
     * @param activityFeedURL
     * @param pageSize
     * @param activities
     * @param since
     */
    private void getFitnessActivityFeed(final UpdateInfo updateInfo, final OAuthService service,
                                        final Token token, String activityFeedURL, final int pageSize,
                                        List<String> activities, long since) throws UpdateFailedException, AuthRevokedException {
        OAuthRequest request = new OAuthRequest(Verb.GET, activityFeedURL);
        request.addQuerystringParameter("pageSize", String.valueOf(pageSize));
        request.addQuerystringParameter("oauth_token", token.getToken());
        request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivityFeed+json");
        final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(DateTimeZone.forID("GMT"));
        if (since>0) {
            final String sinceFormatted = dateFormatter.print(since);
            // add one day of padding to account for unknown timezone
            final String noEarlierFormatted = TimeUtils.dateFormatterUTC.print(since-DateTimeConstants.MILLIS_PER_DAY);
            request.addHeader("If-Modified-Since", sinceFormatted);
            request.addQuerystringParameter("noEarlierThan", noEarlierFormatted);
        }
        service.signRequest(token, request);
        long then = System.currentTimeMillis();
        Response response = request.send();
        final int httpResponseCode = response.getCode();
        String body = response.getBody();
        if (httpResponseCode ==200) {
            JSONObject jsonObject = JSONObject.fromObject(body);
            final JSONArray items = jsonObject.getJSONArray("items");
            for(int i=0; i<items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                final String uri = item.getString("uri");
                if (activityIsAlreadyStored(updateInfo, uri))
                    continue;
                activities.add(uri);
            }
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityFeedURL);
            if (jsonObject.has("next")) {
                activityFeedURL = DEFAULT_ENDPOINT + jsonObject.getString("next");
                getFitnessActivityFeed(updateInfo, service, token, activityFeedURL, pageSize, activities, since);
            }
        } else if (httpResponseCode ==304) {
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityFeedURL);
        } else {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, activityFeedURL, ExceptionUtils.getStackTrace(new Exception()),
                               httpResponseCode, body);
            if (httpResponseCode==403)
                handleTokenRevocation(body);
            if (httpResponseCode>=400&&httpResponseCode<500)
                throw new UpdateFailedException("Unexpected response code: " + httpResponseCode, true, ApiKey.PermanentFailReason.clientError(httpResponseCode));
            else
                throw new UpdateFailedException("Unexpected code: " + httpResponseCode);
        }
    }

    private boolean activityIsAlreadyStored(final UpdateInfo updateInfo, final String uri) {
        final String entityName = JPAUtils.getEntityName(RunKeeperFitnessActivityFacet.class);
        final List<RunKeeperFitnessActivityFacet> facets =
                jpaDaoService.executeQueryWithLimit(String.format("SELECT facet from %s facet WHERE facet.apiKeyId=? AND facet.uri=?", entityName),
                                                    1,
                                                    RunKeeperFitnessActivityFacet.class,
                                                    updateInfo.apiKey.getId(), uri);
        return facets.size()>0;
    }

}
