package com.fluxtream.connectors.runkeeper;

import java.util.ArrayList;
import java.util.List;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import com.fluxtream.aspects.FlxLogger;
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
         objectTypes = {RunKeeperFitnessActivityFacet.class})
public class RunKeeperUpdater  extends AbstractUpdater {

    FlxLogger logger = FlxLogger.getLogger(RunKeeperUpdater.class);

    final String DEFAULT_ENDPOINT= "https://api.runkeeper.com";

    @Autowired
    RunKeeperController runKeeperController;

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        updateData(updateInfo, 0);
        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        "lastUpdated", String.valueOf(System.currentTimeMillis()));
    }

    private void updateData(final UpdateInfo updateInfo, final long since) throws Exception {
        String url = DEFAULT_ENDPOINT+"/user?oauth_token=";
        final String accessToken = updateInfo.apiKey.getAttributeValue("accessToken", env);
        final Token token = new Token(accessToken, env.get("runkeeperConsumerSecret"));
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
        guestService.setApiKeyAttribute(updateInfo.apiKey,
                                        "lastUpdated", String.valueOf(System.currentTimeMillis()));
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

    private void getFitnessActivityFeed(final UpdateInfo updateInfo,
                                        final OAuthService service, final Token token,
                                        String activityFeedURL, final int pageSize,
                                        List<String> activities, long since) {
        OAuthRequest request = new OAuthRequest(Verb.GET, activityFeedURL);
        request.addQuerystringParameter("pageSize", String.valueOf(pageSize));
        request.addQuerystringParameter("oauth_token", token.getToken());
        request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivityFeed+json");
        final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(DateTimeZone.forID("GMT"));
        if (since>0) {
            final String sinceFormatted = dateFormatter.print(since);
            request.addHeader("If-Modified-Since", sinceFormatted);
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
                activities.add(item.getString("uri"));
            }
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, activityFeedURL);
            if (jsonObject.has("next")) {
                activityFeedURL = DEFAULT_ENDPOINT + jsonObject.getString("next");
                getFitnessActivityFeed(updateInfo, service, token, activityFeedURL, pageSize, activities, since);
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

    @Override
    protected void updateConnectorData(final UpdateInfo updateInfo) throws Exception {
        final String lastUpdatedString = guestService.getApiKeyAttribute(updateInfo.apiKey, "lastUpdated");
        final long lastUpdated = Long.valueOf(lastUpdatedString);
        updateData(updateInfo, lastUpdated);
    }

}
