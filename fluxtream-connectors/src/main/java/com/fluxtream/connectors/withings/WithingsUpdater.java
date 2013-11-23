package com.fluxtream.connectors.withings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.AbstractFacet;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.ApiDataService;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
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
         defaultChannels = {"Withings.weight","Withings.systolic", "Withings.diastolic"})
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

    private static final String LAST_ACTIVITY_SYNC_DATE = "lastActivitySyncDate";
    private static final int WEIGHT = 1;
    private static final int HEIGHT = 4;
    private static final int FAT_FREE_MASS = 5;
    private static final int FAT_RATIO = 6;
    private static final int FAT_MASS_WEIGHT = 8;
    private static final int DIASTOLIC_BLOOD_PRESSURE = 9;
    private static final int SYSTOLIC_BLOOD_PRESSURE = 10;
    private static final int HEART_PULSE = 11;

    private enum ApiVersion {
        V1, V2
    }

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
        fetchAndProcessJSON(updateInfo, url, parameters, ApiVersion.V1);

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
        fetchAndProcessJSON(updateInfo, url, parameters, ApiVersion.V1);

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
        fetchAndProcessJSON(updateInfo, urlv2, parameters, ApiVersion.V2);
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
        fetchAndProcessJSON(updateInfo, urlv2, parameters, ApiVersion.V2);
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
                                     final Map<String,String> parameters, ApiVersion apiVersion) throws Exception {
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
            if (!StringUtils.isEmpty(json))
                storeMeasurements(updateInfo, json, apiVersion);
        } catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url, Utils.stackTrace(e), e.getHttpResponseCode(), e.getHttpResponseMessage());
        }
    }

    private void storeMeasurements(final UpdateInfo updateInfo, final String json, final ApiVersion apiVersion) {
        JSONObject jsonObject = JSONObject.fromObject(json);
        Object bodyObject = jsonObject.get("body");
        if (bodyObject==null) return;
        JSONObject body = (JSONObject) bodyObject;
        switch (apiVersion) {
            case V1:
                JSONArray measuregrps = body.getJSONArray("measuregrps");
                for (int i=0; i<measuregrps.size(); i++)
                    storeV1MeasureGroup(updateInfo, measuregrps.getJSONObject(i));
                break;
            case V2:
                Object activitiesObject = body.get("activities");
                if (activitiesObject instanceof JSONObject)
                    storeActivityMeasurement(updateInfo, (JSONObject)activitiesObject);
                else if (activitiesObject instanceof JSONArray) {
                    JSONArray measurements = (JSONArray) activitiesObject;
                    for (int i=0; i<measurements.size(); i++)
                        storeActivityMeasurement(updateInfo, measurements.getJSONObject(i));
                }
                break;
        }
    }

    private void storeV1MeasureGroup(final UpdateInfo updateInfo, final JSONObject measuregrp) {
        final long date = measuregrp.getLong("date")*1000;
        JSONArray measures = measuregrp.getJSONArray ("measures");

        final Connector connector = Connector.getConnector("withings");

        Iterator measuresIterator = measures.iterator();
        final Map<Integer, Float> measuresMap = new HashMap<Integer, Float>();
        while(measuresIterator.hasNext()) {
            JSONObject measure = (net.sf.json.JSONObject) measuresIterator.next();
            double pow = Math.abs (measure.getInt("unit"));
            double measureValue = measure.getDouble("value");
            double divisor = Math.pow (10, pow);
            float fValue = (float)(measureValue / divisor);
            measuresMap.put(measure.getInt("type"), fValue);
        }

        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.start=?",
                                                                                   updateInfo.apiKey.getId(), date);

        if (measuresMap.containsKey(WEIGHT)) {
            final ApiDataService.FacetModifier<WithingsBodyScaleMeasureFacet> facetModifier = new ApiDataService.FacetModifier<WithingsBodyScaleMeasureFacet>() {
                @Override
                public WithingsBodyScaleMeasureFacet createOrModify(WithingsBodyScaleMeasureFacet facet, final Long apiKeyId) {
                    if (facet==null)
                        facet = new WithingsBodyScaleMeasureFacet(updateInfo.apiKey.getId());
                    facet.objectType = ObjectType.getObjectType(connector, "weight").value();
                    facet.measureTime = date;
                    facet.start = date;
                    facet.end = date;
                    facet.weight = measuresMap.get(WEIGHT);
                    facet.height = measuresMap.get(HEIGHT);
                    facet.fatFreeMass = measuresMap.get(FAT_FREE_MASS);
                    facet.fatMassWeight = measuresMap.get(FAT_MASS_WEIGHT);
                    facet.fatRatio = measuresMap.get(FAT_RATIO);
                    return facet;
                }
            };
            final AbstractFacet createdOrModifiedFacet = apiDataService.createOrReadModifyWrite(WithingsBodyScaleMeasureFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
            bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), Arrays.asList(createdOrModifiedFacet));
        }
        if (measuresMap.containsKey(DIASTOLIC_BLOOD_PRESSURE) &&
            measuresMap.containsKey(SYSTOLIC_BLOOD_PRESSURE) &&
            measuresMap.get(DIASTOLIC_BLOOD_PRESSURE)>0f &&
            measuresMap.get(SYSTOLIC_BLOOD_PRESSURE)>0f) {
            final ApiDataService.FacetModifier<WithingsBPMMeasureFacet> facetModifier = new ApiDataService.FacetModifier<WithingsBPMMeasureFacet>() {
                @Override
                public WithingsBPMMeasureFacet createOrModify(WithingsBPMMeasureFacet facet, final Long apiKeyId) {
                    if (facet==null)
                        facet = new WithingsBPMMeasureFacet(updateInfo.apiKey.getId());
                    facet.objectType = ObjectType.getObjectType(connector, "blood_pressure").value();
                    facet.measureTime = date;
                    facet.start = date;
                    facet.end = date;
                    facet.systolic = measuresMap.get(SYSTOLIC_BLOOD_PRESSURE);
                    facet.diastolic = measuresMap.get(DIASTOLIC_BLOOD_PRESSURE);
                    facet.heartPulse = measuresMap.get(HEART_PULSE);
                    return facet;
                }
            };
            final AbstractFacet createdOrModifiedFacet = apiDataService.createOrReadModifyWrite(WithingsBPMMeasureFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
            bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), Arrays.asList(createdOrModifiedFacet));
        }
        if (measuresMap.containsKey(HEART_PULSE)) {
            final ApiDataService.FacetModifier<WithingsHeartPulseMeasureFacet> facetModifier = new ApiDataService.FacetModifier<WithingsHeartPulseMeasureFacet>() {
                @Override
                public WithingsHeartPulseMeasureFacet createOrModify(WithingsHeartPulseMeasureFacet facet, final Long apiKeyId) {
                    if (facet==null)
                        facet = new WithingsHeartPulseMeasureFacet(updateInfo.apiKey.getId());
                    facet.objectType = ObjectType.getObjectType(connector, "heart_pulse").value();
                    facet.start = date;
                    facet.end = date;
                    facet.heartPulse = measuresMap.get(HEART_PULSE);
                    return facet;
                }
            };
            final AbstractFacet createdOrModifiedFacet = apiDataService.createOrReadModifyWrite(WithingsHeartPulseMeasureFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
            bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), Arrays.asList(createdOrModifiedFacet));
        }
    }

    private void storeActivityMeasurement(final UpdateInfo updateInfo, final JSONObject activityData) {
        final String date = activityData.getString("date");
        final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.date=?",
                                                                                   updateInfo.apiKey.getId(), date);
        final ApiDataService.FacetModifier<WithingsActivityFacet> facetModifier = new ApiDataService.FacetModifier<WithingsActivityFacet>() {

            @Override
            public WithingsActivityFacet createOrModify(WithingsActivityFacet facet, final Long apiKeyId) {
                if (facet==null)
                    facet = new WithingsActivityFacet(updateInfo.apiKey.getId());
                extractCommonFacetData(facet, updateInfo);
                facet.date = date;
                facet.timezone = activityData.getString("timezone");
                facet.steps = activityData.getInt("steps");
                facet.calories = (float) activityData.getDouble("calories");
                facet.elevation = (float) activityData.getDouble("elevation");
                return facet;
            };
        };

        final AbstractFacet createdOrModifiedFacet = apiDataService.createOrReadModifyWrite(WithingsActivityFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), Arrays.asList(createdOrModifiedFacet));
    }
}
