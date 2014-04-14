package org.fluxtream.connectors.withings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.Notification;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.TimeUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
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
    private final String WITHINGS_PULSE_LAUNCH_DATE = "2013-06-01";

    private enum ApiVersion {
        V1, V2
    }

    @Autowired
    JPADaoService jpaDaoService;

    public WithingsUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {

        // get user info and find out first seen date
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, WithingsOAuthConnectorController.HAS_UPGRADED_TO_OAUTH)==null) {
            notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(), "Heads Up. This server has recently been upgraded to a version that supports<br>" +
                                                                                                                                                "oauth with the Withings API. Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                                                                                                                "scroll to the Withings connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");
            // Record permanent failure since this connector won't work again until
            // it is reauthenticated
            guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
            throw new UpdateFailedException("requires token reauthorization",true);
        }

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
            notificationsService.addNamedNotification(updateInfo.getGuestId(), Notification.Type.WARNING, connector().statusNotificationName(), "Heads Up. This server has recently been upgraded to a version that supports<br>" +
                                                                                                                                                "oauth with the Withings API. Please head to <a href=\"javascript:App.manageConnectors()\">Manage Connectors</a>,<br>" +
                                                                                                                                                "scroll to the Withings connector, and renew your tokens (look for the <i class=\"icon-resize-small icon-large\"></i> icon)");
            // Record permanent failure since this connector won't work again until
            // it is reauthenticated
            guestService.setApiKeyStatus(updateInfo.apiKey.getId(), ApiKey.Status.STATUS_PERMANENT_FAILURE, null);
            throw new UpdateFailedException("requires token reauthorization",true);
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
        final String todaysDate = TimeUtils.dateFormatterUTC.print(System.currentTimeMillis());
        String urlv2 = "http://wbsapi.withings.net/v2/measure";
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("action", "getactivity");
        parameters.put("userid", userid);
        parameters.put("startdateymd", WITHINGS_PULSE_LAUNCH_DATE);
        parameters.put("enddateymd", String.valueOf(todaysDate));
        fetchAndProcessJSON(updateInfo, urlv2, parameters, ApiVersion.V2);
        final String lastActivityDate = getLastActivityDate(updateInfo);
        guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE, lastActivityDate);
    }

    private void getRecentActivityData(final UpdateInfo updateInfo, final String userid) throws Exception {
        final String todaysDate = TimeUtils.dateFormatterUTC.print(System.currentTimeMillis());
        final String lastActivitySyncDate = guestService.getApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE);
        String urlv2 = String.format("http://wbsapi.withings.net/v2/measure", userid, lastActivitySyncDate, todaysDate);
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put("action", "getactivity");
        parameters.put("userid", userid);
        parameters.put("startdateymd", lastActivitySyncDate);
        parameters.put("enddateymd", String.valueOf(todaysDate));
        fetchAndProcessJSON(updateInfo, urlv2, parameters, ApiVersion.V2);
        final String lastActivityDate = getLastActivityDate(updateInfo);
        guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE, lastActivityDate);
    }

    private String getLastActivityDate(final UpdateInfo updateInfo) {
        final String entityName = JPAUtils.getEntityName(WithingsActivityFacet.class);
        final List<WithingsActivityFacet> facets =
                jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.date DESC", 1, WithingsActivityFacet.class, updateInfo.apiKey.getId());
        if (facets.size()==0) return WITHINGS_PULSE_LAUNCH_DATE;
        return facets.get(0).date;
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
    public OAuthService getOAuthService(final ApiKey apiKey) {
        return new ServiceBuilder()
                .provider(WithingsApi.class)
                .apiKey(guestService.getApiKeyAttribute(apiKey, "withingsConsumerKey"))
                .apiSecret(guestService.getApiKeyAttribute(apiKey, "withingsConsumerSecret"))
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
            OAuthService service = getOAuthService(updateInfo.apiKey);
            final String accessToken = guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken");
            final Token token = new Token(accessToken, guestService.getApiKeyAttribute(updateInfo.apiKey, "tokenSecret"));
            service.signRequest(token, request);
            Response response = request.send();
            final int httpResponseCode = response.getCode();
            if (httpResponseCode!=200)
                throw new UnexpectedHttpResponseCodeException(httpResponseCode, response.getBody());
            String json = response.getBody();
            JSONObject jsonObject = JSONObject.fromObject(json);
            if (jsonObject.getInt("status")!=0)
                throw new UnexpectedHttpResponseCodeException(jsonObject.getInt("status"), "Unexpected status code: " + jsonObject.getInt("status"));
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
            if (!StringUtils.isEmpty(json))
                storeMeasurements(updateInfo, json, apiVersion);
        } catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url, Utils.stackTrace(e), e.getHttpResponseCode(), e.getHttpResponseMessage());
            throw e;
        }
    }

    private void storeMeasurements(final UpdateInfo updateInfo, final String json, final ApiVersion apiVersion) throws Exception {
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

    private void storeV1MeasureGroup(final UpdateInfo updateInfo, final JSONObject measuregrp) throws Exception {
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
                    extractCommonFacetData(facet, updateInfo);
                    if (measuresMap.get(HEIGHT)!=null)
                        facet.height = measuresMap.get(HEIGHT);
                    if (measuresMap.get(FAT_FREE_MASS)!=null)
                        facet.fatFreeMass = measuresMap.get(FAT_FREE_MASS);
                    if (measuresMap.get(FAT_MASS_WEIGHT)!=null)
                    facet.fatMassWeight = measuresMap.get(FAT_MASS_WEIGHT);
                    if (measuresMap.get(FAT_RATIO)!=null)
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
                    extractCommonFacetData(facet, updateInfo);
                    facet.objectType = ObjectType.getObjectType(connector, "blood_pressure").value();
                    facet.measureTime = date;
                    facet.start = date;
                    facet.end = date;
                    facet.systolic = measuresMap.get(SYSTOLIC_BLOOD_PRESSURE);
                    facet.diastolic = measuresMap.get(DIASTOLIC_BLOOD_PRESSURE);
                    if (measuresMap.get(HEART_PULSE)!=null)
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
                    extractCommonFacetData(facet, updateInfo);
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

    private void storeActivityMeasurement(final UpdateInfo updateInfo, final JSONObject activityData) throws Exception {
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

                final DateTime dateTime = TimeUtils.dateFormatterUTC.parseDateTime(facet.date);

                // returns the starting midnight for the date
                facet.start = dateTime.getMillis();
                facet.end = dateTime.getMillis()+ DateTimeConstants.MILLIS_PER_DAY-1;

                facet.startTimeStorage = facet.date + "T00:00:00.000";
                facet.endTimeStorage = facet.date + "T23:59:59.999";

                if (activityData.has("timezone"))
                    facet.timezone = activityData.getString("timezone");
                if (activityData.has("steps"))
                    facet.steps = activityData.getInt("steps");
                if (activityData.has("distance"))
                    facet.distance = (float) activityData.getDouble("distance");
                if (activityData.has("calories"))
                   facet.calories = (float) activityData.getDouble("calories");
                if (activityData.has("elevation"))
                    facet.elevation = (float) activityData.getDouble("elevation");
                return facet;
            };
        };
        final AbstractFacet createdOrModifiedFacet = apiDataService.createOrReadModifyWrite(WithingsActivityFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), Arrays.asList(createdOrModifiedFacet));
    }
}
