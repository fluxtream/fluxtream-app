package com.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.services.MetadataService;
import com.fluxtream.utils.JPAUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
         objectTypes = {RunKeeperFitnessActivityFacet.class, LocationFacet.class})
public class RunKeeperUpdater  extends AbstractUpdater {

    final String DEFAULT_ENDPOINT= "https://api.runkeeper.com";

    @Autowired
    RunKeeperController runKeeperController;

    @Autowired
    JPADaoService jpaDaoService;

    final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss");

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
        long lastUpdated = 0;
        if (newest.size()>0)
            lastUpdated = newest.get(0).end;
        else
            throw new Exception("Unexpected Error: no existing facets with an incremental update");
        System.out.println("Runkeeper's was last updated" + timeFormatter.print(lastUpdated));
        updateData(updateInfo, lastUpdated);
    }

    private void updateData(final UpdateInfo updateInfo, final long since) throws Exception {
        String url = DEFAULT_ENDPOINT+"/user?oauth_token=";
        final String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
        final Token token = new Token(accessToken, guestService.getApiKeyAttribute(updateInfo.apiKey, "runkeeperConsumerSecret"));
        final String userEndpoint = url + accessToken;
        OAuthRequest request = new OAuthRequest(Verb.GET, userEndpoint);
        request.addHeader("Accept", "application/vnd.com.runkeeper.User+json");
        final OAuthService service = runKeeperController.getOAuthService();
        service.signRequest(token, request);
        Response response = request.send();
        String body = response.getBody();
        JSONObject jsonObject = JSONObject.fromObject(body);
        String fitnessActivities = jsonObject.getString("fitness_activities");
        List<String> activities = new ArrayList<String>();
        String activityFeedURL = DEFAULT_ENDPOINT + fitnessActivities;

        getFitnessActivityFeed(updateInfo, service, token, activityFeedURL, 25, activities, since);
        getFitnessActivities(updateInfo, service, token, activities);
    }

    private void getFitnessActivities(final UpdateInfo updateInfo, final OAuthService service,
                                      final Token token, final List<String> activities) throws Exception {
        for (String activity : activities) {
            String activityURL = DEFAULT_ENDPOINT + activity;
            OAuthRequest request = new OAuthRequest(Verb.GET, activityURL);
            request.addQuerystringParameter("oauth_token", token.getToken());
            request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivity+json");
            service.signRequest(token, request);
            long then = System.currentTimeMillis();
            Response response = request.send();
            final int httpResponseCode = response.getCode();
            if (httpResponseCode ==200) {
                countSuccessfulApiCall(updateInfo.apiKey,
                                       updateInfo.objectTypes, then, activityURL);
                String body = response.getBody();
                JSONObject jsonObject = JSONObject.fromObject(body);
                createOrUpdateActivity(jsonObject, updateInfo);
            } else {
                countFailedApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityURL, "",
                                   httpResponseCode, response.getBody());
                throw new RuntimeException("Unexpected code: " + httpResponseCode);
            }
        }
    }

    private void createOrUpdateActivity(final JSONObject jsonObject, final UpdateInfo updateInfo) {
        final String uri = jsonObject.getString("uri");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.uri=?",
                                                                                   updateInfo.apiKey.getId(), uri);
        final ApiDataService.FacetModifier<RunKeeperFitnessActivityFacet> facetModifier = new ApiDataService.FacetModifier<RunKeeperFitnessActivityFacet>() {
            @Override
            public RunKeeperFitnessActivityFacet createOrModify(RunKeeperFitnessActivityFacet facet, final Long apiKeyId) {
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
                    final JSONArray heart_rateArray = jsonObject.getJSONArray("heart_rate");
                    HeartRateMeasure heartRateMeasure = new HeartRateMeasure();
                    facet.heart_rate = new ArrayList<HeartRateMeasure>(heart_rateArray.size());
                    for (int i=0; i<heart_rateArray.size(); i++) {
                        final JSONObject heartRateTuple = heart_rateArray.getJSONObject(i);
                        heartRateMeasure.timestamp = heartRateTuple.getDouble("timestamp");
                        heartRateMeasure.heartRate = heartRateTuple.getDouble("heart_rate");
                        facet.heart_rate.add(heartRateMeasure);
                    }
                }
                if (jsonObject.has("calories")) {
                    // ignore calories for now
                }
                return facet;
            }
        };
        apiDataService.createOrReadModifyWrite(RunKeeperFitnessActivityFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
    }

    /**
     * Get the feed of activities in a succint format. FitnessActivity info (with gps data etc) is fetched in a separate call
     * (one per activity). We want to limit this feed to those activities that we haven't already stored of course but
     * unfortunately the Runkeeper API call will by default retrieve the entire feed. Optional parameters
     * (<code>noEarlierThan</code>, <code>noLaterThan</code>) are able to limit the dataset, but they will only accept dates specified in
     * <code>yyyy-MM-DD</code> format, which obviously limits the boundary limits granularity to a day. Additionally, it is unclear
     * what timezone is used to filter the dataset (is it GMT, that is then converted to the local time, or is the
     * parameter given in local time?). Consequently, we use the <code>noEarlierThan</code> parameter with a one day padding and
     * further filter the dataset using the list of activity that we already have data for (<code>uriList</code>).
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
                                        List<String> activities, long since) {
        OAuthRequest request = new OAuthRequest(Verb.GET, activityFeedURL);
        request.addQuerystringParameter("pageSize", String.valueOf(pageSize));
        request.addQuerystringParameter("oauth_token", token.getToken());
        request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivityFeed+json");
        final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(DateTimeZone.forID("GMT"));
        final DateTimeFormatter simpleDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.forID("GMT"));
        if (since>0) {
            final String sinceFormatted = dateFormatter.print(since);
            // add one day of padding to account for unknown timezone
            final String noEarlierFormatted = simpleDateFormatter.print(since-DateTimeConstants.MILLIS_PER_DAY);
            request.addHeader("If-Modified-Since", sinceFormatted);
            request.addQuerystringParameter("noEarlierThan", noEarlierFormatted);
        }
        service.signRequest(token, request);
        long then = System.currentTimeMillis();
        Response response = request.send();
        final int httpResponseCode = response.getCode();
        if (httpResponseCode ==200) {
            String body = response.getBody();
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
                               updateInfo.objectTypes, then, activityFeedURL, "",
                               httpResponseCode, response.getBody());
            throw new RuntimeException("Unexpected code: " + httpResponseCode);
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
