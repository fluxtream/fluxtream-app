package com.fluxtream.connectors.withings;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.WithingsApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Withings", value = 4, objectTypes = {
        WithingsBPMMeasureFacet.class, WithingsBodyScaleMeasureFacet.class, WithingsHeartPulseMeasureFacet.class,
            WithingsActivityFacet.class},
         extractor = WithingsFacetExtractor.class,
         defaultChannels = {"Withings.weight","Withings.systolic", "Withings.diastolic"})
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

    private static final String LAST_ACTIVITY_SYNC_DATE = "lastActivitySyncDate";

    @Autowired
    JPADaoService jpaDaoService;

    protected transient DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    public WithingsUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        // get user info and find out first seen date

        final String userid = guestService.getApiKeyAttribute(updateInfo.apiKey, "userid");

        // do v1 API call
        String url = "http://wbsapi.withings.net/measure";
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("action", "getmeas");
        parameters.put("userid", userid);
        parameters.put("startdate", "0");
        parameters.put("enddate", String.valueOf(System.currentTimeMillis() / 1000));
        fetchAndProcessJSON(updateInfo, url, parameters);

        // do v2 (activity) API call
        getActivityDataHistory(updateInfo, userid);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        long lastBodyscaleMeasurement = getLastBodyScaleMeasurement(updateInfo);
        long lastBloodPressureMeasurement = getLastBloodPressureMeasurement(updateInfo);

        long lastMeasurement = Math.max(lastBodyscaleMeasurement, lastBloodPressureMeasurement);

        final String userid = guestService.getApiKeyAttribute(updateInfo.apiKey, "userid");
        final long startdate = lastMeasurement / 1000;
        final long enddate = System.currentTimeMillis() / 1000;

        if (guestService.getApiKeyAttribute(updateInfo.apiKey, WithingsOAuthConnectorController.HAS_UPGRADED_TO_OAUTH)==null) {
            notificationsService.addNotification(updateInfo.getGuestId(), Notification.Type.WARNING,
                                                 "Heads Up. This server has recently been upgraded to a version that supports<br>" +
                                                 "oauth with the Withings API. Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                 "scroll to the Withings connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");
            return;
        }

        // do v1 API call
        String url = "http://wbsapi.withings.net/measure";
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("action", "getmeas");
        parameters.put("userid", userid);
        parameters.put("startdate", String.valueOf(startdate));
        parameters.put("enddate", String.valueOf(enddate));
        fetchAndProcessJSON(updateInfo, url, parameters);

        // do v2 (activity) API call
        final String lastActivitySyncDate = guestService.getApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE);
        if (lastActivitySyncDate ==null)
            getActivityDataHistory(updateInfo, userid);
        else
            getRecentActivityData(updateInfo, userid);
    }

    private void getActivityDataHistory(final UpdateInfo updateInfo, final String userid) throws Exception {
        final String todaysDate = dateFormatter.withZoneUTC().print(System.currentTimeMillis());
        String urlv2 = "http://wbsapi.withings.net/v2/measure";
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("action", "getactivity");
        parameters.put("userid", userid);
        parameters.put("startdateymd", "2013-06-01");
        parameters.put("enddateymd", String.valueOf(todaysDate));
        fetchAndProcessJSON(updateInfo, urlv2, parameters);
        guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE, todaysDate);
    }

    private void getRecentActivityData(final UpdateInfo updateInfo, final String userid) throws Exception {
        final String todaysDate = dateFormatter.withZoneUTC().print(System.currentTimeMillis());
        final String lastActivitySyncDate = guestService.getApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE);
        String urlv2 = String.format("http://wbsapi.withings.net/v2/measure", userid, lastActivitySyncDate, todaysDate);
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("action", "getactivity");
        parameters.put("userid", userid);
        parameters.put("startdateymd", lastActivitySyncDate);
        parameters.put("enddateymd", String.valueOf(todaysDate));
        fetchAndProcessJSON(updateInfo, urlv2, parameters);
        guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE, todaysDate);
    }

    private long getLastBloodPressureMeasurement(final UpdateInfo updateInfo) {
        final String entityName = JPAUtils.getEntityName(WithingsBPMMeasureFacet.class);
        final List<WithingsBPMMeasureFacet> facets =
                jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC",
                                                    1,
                                                    WithingsBPMMeasureFacet.class,
                                                    updateInfo.apiKey.getId());
        if (facets.size()==0) return 0;
        return facets.get(0).start + 1000;
    }

    private long getLastBodyScaleMeasurement(final UpdateInfo updateInfo) {
        final String entityName = JPAUtils.getEntityName(WithingsBodyScaleMeasureFacet.class);
        final List<WithingsBodyScaleMeasureFacet> facets =
                jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC",
                                                    1,
                                                    WithingsBodyScaleMeasureFacet.class,
                                                    updateInfo.apiKey.getId());
        if (facets.size()==0) return 0;
        return facets.get(0).start + 1000;
    }
    public OAuthService getOAuthService() {
        return new ServiceBuilder()
                .provider(WithingsApi.class)
                .apiKey(env.get("withingsConsumerKey"))
                .apiSecret(env.get("withingsConsumerSecret"))
                .signatureType(SignatureType.QueryString)
                .callback(env.get("homeBaseUrl") + "withings/upgradeToken")
                .build();
    }

    private void fetchAndProcessJSON(final UpdateInfo updateInfo, final String url,
                                     final Map<String,String> parameters) throws Exception {
        long then = System.currentTimeMillis();
        try {
            OAuthRequest request = new OAuthRequest(Verb.GET, url);
            for (String parameterName : parameters.keySet()) {
                request.addQuerystringParameter(parameterName,
                                                parameters.get(parameterName));
            }
            OAuthService service = getOAuthService();
            final String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
            final Token token = new Token(accessToken, guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenSecret"));
            service.signRequest(token, request);
            Response response = request.send();
            if (response.getCode()!=200)
                throw new UnexpectedHttpResponseCodeException(response.getCode(), response.getBody());
            String json = response.getBody();
            JSONObject jsonObject = JSONObject.fromObject(json);
            if (jsonObject.getInt("status")!=0)
                throw new UnexpectedHttpResponseCodeException(jsonObject.getInt("status"), "Unexpected status code: " + jsonObject.getInt("status"));
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
            if (StringUtils.isEmpty(json))
                apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
        } catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, url, Utils.stackTrace(e),
                               e.getHttpResponseCode(), e.getHttpResponseMessage());
        } catch (IOException e) {
            reportFailedApiCall(updateInfo.apiKey,
                                updateInfo.objectTypes, then, url, Utils.stackTrace(e), "I/O");
            throw e;
        }
    }
}
