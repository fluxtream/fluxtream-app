package com.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.location.LocationFacet;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.services.JPADaoService;
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

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        updateData(updateInfo, 0);
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

        final List<String> uriList = getActivityUriList(updateInfo.apiKey);
        getFitnessActivityFeed(updateInfo, service, token, activityFeedURL, 25, activities, since, uriList);
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
            if (response.getCode()==200) {
                countSuccessfulApiCall(updateInfo.apiKey,
                                       updateInfo.objectTypes, then, activityURL);
                String body = response.getBody();
                apiDataService.cacheApiDataJSON(updateInfo, body, -1, -1);
            } else {
                countFailedApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityURL, "");
                throw new RuntimeException("Unexpected code: " + response.getCode());
            }
        }
    }

    /**
     * Get the feed of activities in a succint format. Activity info (with gps data etc) is fetched in a separate call
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
     * @param uriList
     */
    private void getFitnessActivityFeed(final UpdateInfo updateInfo, final OAuthService service, final Token token, String activityFeedURL, final int pageSize, List<String> activities, long since, final List<String> uriList) {
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
        if (response.getCode()==200) {
            String body = response.getBody();
            JSONObject jsonObject = JSONObject.fromObject(body);
            final JSONArray items = jsonObject.getJSONArray("items");
            for(int i=0; i<items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                final String uri = item.getString("uri");
                if (uriList.contains(uri))
                    continue;
                activities.add(uri);
            }
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityFeedURL);
            if (jsonObject.has("next")) {
                activityFeedURL = DEFAULT_ENDPOINT + jsonObject.getString("next");
                getFitnessActivityFeed(updateInfo, service, token, activityFeedURL, pageSize, activities, since, uriList);
            }
        } else if (response.getCode()==304) {
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityFeedURL);
        } else {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, activityFeedURL, "");
            throw new RuntimeException("Unexpected code: " + response.getCode());
        }
    }

    /**
     * retrieve the 25 last activity uris that we already have in store
     * @param apiKey
     * @return a list of activity uris
     */
    protected List<String> getActivityUriList(ApiKey apiKey) {
        final String entityName = JPAUtils.getEntityName(RunKeeperFitnessActivityFacet.class);
        final List<RunKeeperFitnessActivityFacet> facets = jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC", 25, RunKeeperFitnessActivityFacet.class, apiKey.getId());
        List<String> uris = new ArrayList<String>();
        for (RunKeeperFitnessActivityFacet facet : facets) {
            uris.add(facet.uri);
        }
        return uris;
    }

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        final String entityName = JPAUtils.getEntityName(RunKeeperFitnessActivityFacet.class);
        final List<RunKeeperFitnessActivityFacet> newest = jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC", 1, RunKeeperFitnessActivityFacet.class, updateInfo.apiKey.getId());
        long lastUpdated = 0;
        if (newest.size()>0)
            lastUpdated = newest.get(0).end;
        else
            throw new Exception("Unexpected Error: no existing facets with an incremental update");
        updateData(updateInfo, lastUpdated);
    }

}
