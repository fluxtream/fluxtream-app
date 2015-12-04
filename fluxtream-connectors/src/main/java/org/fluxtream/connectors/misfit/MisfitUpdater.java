package org.fluxtream.connectors.misfit;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Autonomous;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.*;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.JPAUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DurationFieldType;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by candide on 09/02/15.
 */
@Component
@Updater(prettyName = "Misfit", value = 8, objectTypes = {MisfitActivitySummaryFacet.class, MisfitActivitySessionFacet.class, MisfitSleepFacet.class},
        bodytrackResponder = MisfitBodytrackResponder.class,
        defaultChannels = {"Misfit.steps"}
)
public class MisfitUpdater extends AbstractUpdater implements Autonomous {

    @PersistenceContext
    EntityManager em;

    @Autowired
    BodyTrackHelper bodytrackHelper;

    private final String SESSION_HISTORY_COMPLETE_ATTKEY = "sessionHistoryComplete";
    private final String SLEEP_HISTORY_COMPLETE_ATTKEY = "sleepHistoryComplete";
    private final String BACKFILL_ENDDATE_ATTKEY_PREFIX = "backFillEndDate_";
    FlxLogger logger = FlxLogger.getLogger(MisfitUpdater.class);
    private String SUMMARY_HISTORY_COMPLETE_ATTKEY = "summaryHistoryComplete";


    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        boolean summaryHistoryComplete = isTrue(guestService.getApiKeyAttribute(updateInfo.apiKey, SUMMARY_HISTORY_COMPLETE_ATTKEY));
        boolean sessionHistoryComplete = isTrue(guestService.getApiKeyAttribute(updateInfo.apiKey, SESSION_HISTORY_COMPLETE_ATTKEY));
        boolean sleepHistoryComplete = isTrue(guestService.getApiKeyAttribute(updateInfo.apiKey, SLEEP_HISTORY_COMPLETE_ATTKEY));
        // provide one day of padding to account for all timezones
        if (!summaryHistoryComplete) {
            backwardRetrieveMisfitHistoryData(updateInfo, ObjectType.getObjectTypeValue(MisfitActivitySummaryFacet.class));
            guestService.setApiKeyAttribute(updateInfo.apiKey, SUMMARY_HISTORY_COMPLETE_ATTKEY, "true");
        }
        if (!sessionHistoryComplete) {
            backwardRetrieveMisfitHistoryData(updateInfo, ObjectType.getObjectTypeValue(MisfitActivitySessionFacet.class));
            guestService.setApiKeyAttribute(updateInfo.apiKey, SESSION_HISTORY_COMPLETE_ATTKEY, "true");
        }
        if (!sleepHistoryComplete) {
            backwardRetrieveMisfitHistoryData(updateInfo, ObjectType.getObjectTypeValue(MisfitSleepFacet.class));
            guestService.setApiKeyAttribute(updateInfo.apiKey, SLEEP_HISTORY_COMPLETE_ATTKEY, "true");
        }
    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        String lastSummaryDate = getLastSummaryDate(updateInfo);

        // check for null last dates in case there was no data yet at the time of the history update

        if (lastSummaryDate==null)
            backwardRetrieveMisfitData(updateInfo, ObjectType.getObjectTypeValue(MisfitActivitySummaryFacet.class));
        else {
            DateTime maxAllowedEndDate = getMaxAllowedEndDate(lastSummaryDate);
            retrieveMisfitDataFromTo(updateInfo, ObjectType.getObjectTypeValue(MisfitActivitySummaryFacet.class), lastSummaryDate, ISODateTimeFormat.date().print(maxAllowedEndDate));
        }

        String lastSessionDate = getLastSessionDate(updateInfo);
        if (lastSessionDate==null) {
            backwardRetrieveMisfitData(updateInfo, ObjectType.getObjectTypeValue(MisfitActivitySessionFacet.class));
        } else {
            DateTime maxAllowedEndDate = getMaxAllowedEndDate(lastSessionDate);
            retrieveMisfitDataFromTo(updateInfo, ObjectType.getObjectTypeValue(MisfitActivitySessionFacet.class), lastSessionDate, ISODateTimeFormat.date().print(maxAllowedEndDate));
        }

        String lastSleepDate = getLastSleepDate(updateInfo);
        if (lastSleepDate==null)
            backwardRetrieveMisfitData(updateInfo, ObjectType.getObjectTypeValue(MisfitSleepFacet.class));
        else {
            DateTime maxAllowedEndDate = getMaxAllowedEndDate(lastSleepDate);
            retrieveMisfitDataFromTo(updateInfo, ObjectType.getObjectTypeValue(MisfitSleepFacet.class), lastSleepDate, ISODateTimeFormat.date().print(maxAllowedEndDate));
        }
    }

    private DateTime getMaxAllowedEndDate(String lastSummaryDate) {
        DateTime thirtyDaysLater = (ISODateTimeFormat.date().parseDateTime(lastSummaryDate).plusDays(30));
        DateTime now = DateTime.now();
        return thirtyDaysLater.isAfter(now)?now:thirtyDaysLater;
    }

    private String getLastSummaryDate(UpdateInfo updateInfo) {
        Query nativeQuery = em.createNativeQuery(String.format("SELECT max(date) FROM %s WHERE apiKeyId=?", JPAUtils.getEntityName(MisfitActivitySummaryFacet.class)));
        nativeQuery.setParameter(1, updateInfo.apiKey.getId());
        List<Object> resultList = nativeQuery.getResultList();
        if (resultList.size()==0) return null;
        return (String) resultList.get(0);
    }

    private String getLastSessionDate(UpdateInfo updateInfo) {
        Query nativeQuery = em.createNativeQuery(String.format("SELECT max(start) FROM %s WHERE apiKeyId=?", JPAUtils.getEntityName(MisfitActivitySessionFacet.class)));
        nativeQuery.setParameter(1, updateInfo.apiKey.getId());
        List<Object> resultList = nativeQuery.getResultList();
        if (resultList.size()==0) return null;
        long start = ((BigInteger) resultList.get(0)).longValue();
        return ISODateTimeFormat.date().print(start-DateTimeConstants.MILLIS_PER_DAY);
    }

    private String getLastSleepDate(UpdateInfo updateInfo) {
        Query nativeQuery = em.createNativeQuery(String.format("SELECT max(start) FROM %s WHERE apiKeyId=?", JPAUtils.getEntityName(MisfitSleepFacet.class)));
        nativeQuery.setParameter(1, updateInfo.apiKey.getId());
        List<Object> resultList = nativeQuery.getResultList();
        long start = ((BigInteger) resultList.get(0)).longValue();
        return ISODateTimeFormat.date().print(start-DateTimeConstants.MILLIS_PER_DAY);
    }

    private void backwardRetrieveMisfitHistoryData(UpdateInfo updateInfo, int objectTypeValue) throws Exception {
        while (true) {
            boolean existingData = backwardRetrieveMisfitDataChunk(updateInfo, objectTypeValue);
            // retrieve everyting after july 1st 2012
            String startDate = guestService.getApiKeyAttribute(updateInfo.apiKey, BACKFILL_ENDDATE_ATTKEY_PREFIX + ObjectType.getObjectType(connector(), objectTypeValue).name());
            if (ISODateTimeFormat.date().parseDateTime(startDate).isBefore(ISODateTimeFormat.date().parseDateTime("2012-07-01")))
                break;
        }
    }

    private void backwardRetrieveMisfitData(UpdateInfo updateInfo, int objectTypeValue) throws Exception {
        while (true) {
            boolean existingData = backwardRetrieveMisfitDataChunk(updateInfo, objectTypeValue);
            if (!existingData) break;
        }
    }

    private boolean backwardRetrieveMisfitDataChunk(UpdateInfo updateInfo, int objectTypeValue) throws Exception {
        String endDate = getBackfillEndDate(updateInfo, ObjectType.getObjectType(connector(), objectTypeValue));
        String startDate = ISODateTimeFormat.date().print(ISODateTimeFormat.date().parseLocalDate(endDate).minusDays(30));
        retrieveMisfitDataFromTo(updateInfo, objectTypeValue, startDate, endDate);
        // if everything went well set backfill-endDate to startDate
        guestService.setApiKeyAttribute(updateInfo.apiKey, BACKFILL_ENDDATE_ATTKEY_PREFIX + ObjectType.getObjectType(connector(), objectTypeValue).name(), startDate);
        return false;
    }

    private boolean retrieveMisfitDataFromTo(UpdateInfo updateInfo, int objectTypeValue, String startDate, String endDate) throws Exception {
        String misfitDataTypeName;
        if (objectTypeValue==ObjectType.getObjectTypeValue(MisfitActivitySummaryFacet.class))
            misfitDataTypeName = "summary";
        else if (objectTypeValue==ObjectType.getObjectTypeValue(MisfitActivitySessionFacet.class))
            misfitDataTypeName = "sessions";
        else if (objectTypeValue==ObjectType.getObjectTypeValue(MisfitSleepFacet.class))
            misfitDataTypeName = "sleeps";
        else
            throw new RuntimeException("Unknown objectTypeValue: " + objectTypeValue);
        String json = makeRestCall(updateInfo, objectTypeValue,
                String.format("https://api.misfitwearables.com/move/resource/v1/user/me/activity/%s?start_date=%s&end_date=%s&detail=true", misfitDataTypeName, startDate, endDate));
        JSONObject jsonApiData = JSONObject.fromObject(json);
        JSONArray misfitData = getMisfitData(jsonApiData, misfitDataTypeName);
        if (misfitData.size()==0) return false;
        extractFacets(updateInfo, misfitData, objectTypeValue);
        return true;
    }

    private void extractFacets(UpdateInfo updateInfo, JSONArray misfitData, int objectTypeValue) throws Exception {
        List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
        for (int i=0; i<misfitData.size(); i++) {
            JSONObject misfitJson = misfitData.getJSONObject(i);
            AbstractFacet facet;
            if (objectTypeValue==ObjectType.getObjectTypeValue(MisfitActivitySummaryFacet.class))
                facet = createOrUpdateActivitySummaryFacet(misfitJson, updateInfo);
            else if (objectTypeValue==ObjectType.getObjectTypeValue(MisfitActivitySessionFacet.class))
                facet = createOrUpdateActivitySessionFacet(misfitJson, updateInfo);
            else if (objectTypeValue==ObjectType.getObjectTypeValue(MisfitSleepFacet.class))
                facet = createOrUpdateSleepFacet(misfitJson, updateInfo);
            else
                throw new RuntimeException("Unknown objectTypeValue: " + objectTypeValue);
            if (facet!=null)
                newFacets.add(facet);
        }
        bodyTrackStorageService.storeApiData(updateInfo.apiKey, newFacets);
    }

    private MisfitActivitySummaryFacet createOrUpdateActivitySummaryFacet(final JSONObject misfitJson, final UpdateInfo updateInfo) throws Exception {
        final String date = misfitJson.getString("date");

        MisfitActivitySummaryFacet ret =
                apiDataService.createOrReadModifyWrite(MisfitActivitySummaryFacet.class,
                        new ApiDataService.FacetQuery(
                                "e.apiKeyId = ? AND e.date = ?",
                                updateInfo.apiKey.getId(),
                                date),
                        new ApiDataService.FacetModifier<MisfitActivitySummaryFacet>() {
                            // Throw exception if it turns out we can't make sense of the observation's JSON
                            // This will abort the transaction
                            @Override
                            public MisfitActivitySummaryFacet createOrModify(MisfitActivitySummaryFacet facet, Long apiKeyId) {
                                if (facet == null) {
                                    facet = new MisfitActivitySummaryFacet(updateInfo.apiKey.getId());
                                    facet.date = date;
                                    extractCommonFacetData(facet, updateInfo);
                                }
                                facet.startTimeStorage = date + "T00:00:00Z";
                                facet.endTimeStorage = date + "T23:59:59Z";
                                facet.start = ISODateTimeFormat.dateTimeNoMillis().parseMillis(facet.startTimeStorage);
                                facet.end = ISODateTimeFormat.dateTimeNoMillis().parseMillis(facet.endTimeStorage);
                                facet.points = (float) misfitJson.getDouble("points");
                                facet.steps = misfitJson.getInt("steps");
                                facet.calories = (float) misfitJson.getDouble("calories");
                                facet.activityCalories = (float) misfitJson.getDouble("activityCalories");
                                facet.distance = (float) misfitJson.getDouble("distance");
                                return facet;
                            }
                        }, updateInfo.apiKey.getId());
        return ret;
    }

    private MisfitActivitySessionFacet createOrUpdateActivitySessionFacet(final JSONObject misfitJson, final UpdateInfo updateInfo) throws Exception {
        final String misfitId = misfitJson.getString("id");

        MisfitActivitySessionFacet ret =
                apiDataService.createOrReadModifyWrite(MisfitActivitySessionFacet.class,
                        new ApiDataService.FacetQuery(
                                "e.apiKeyId = ? AND e.misfitId = ?",
                                updateInfo.apiKey.getId(),
                                misfitId),
                        new ApiDataService.FacetModifier<MisfitActivitySessionFacet>() {
                            // Throw exception if it turns out we can't make sense of the observation's JSON
                            // This will abort the transaction
                            @Override
                            public MisfitActivitySessionFacet createOrModify(MisfitActivitySessionFacet facet, Long apiKeyId) {
                                if (facet == null) {
                                    facet = new MisfitActivitySessionFacet(updateInfo.apiKey.getId());
                                    facet.misfitId = misfitId;
                                    extractCommonFacetData(facet, updateInfo);
                                }
                                facet.activityType = misfitJson.getString("activityType");
                                facet.start = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(misfitJson.getString("startTime")).getMillis();
                                facet.end = facet.start + misfitJson.getInt("duration")*1000;
                                facet.points = (float) misfitJson.getDouble("points");
                                facet.steps = misfitJson.getInt("steps");
                                facet.calories = (float) misfitJson.getDouble("calories");
                                facet.distance = (float) misfitJson.getDouble("distance");
                                return facet;
                            }
                        }, updateInfo.apiKey.getId());
        return ret;
    }

    private MisfitSleepFacet createOrUpdateSleepFacet(final JSONObject misfitJson, final UpdateInfo updateInfo) throws Exception {
        final String misfitId = misfitJson.getString("id");

        MisfitSleepFacet ret =
                apiDataService.createOrReadModifyWrite(MisfitSleepFacet.class,
                        new ApiDataService.FacetQuery(
                                "e.apiKeyId = ? AND e.misfitId = ?",
                                updateInfo.apiKey.getId(),
                                misfitId),
                        new ApiDataService.FacetModifier<MisfitSleepFacet>() {
                            // Throw exception if it turns out we can't make sense of the observation's JSON
                            // This will abort the transaction
                            @Override
                            public MisfitSleepFacet createOrModify(MisfitSleepFacet facet, Long apiKeyId) {
                                if (facet == null) {
                                    facet = new MisfitSleepFacet(updateInfo.apiKey.getId());
                                    facet.misfitId = misfitId;
                                    extractCommonFacetData(facet, updateInfo);
                                }
                                int duration = misfitJson.getInt("duration");
                                DateTime startTime = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(misfitJson.getString("startTime"));
                                facet.date = ISODateTimeFormat.date().print(startTime.withFieldAdded(DurationFieldType.seconds(), duration));
                                facet.start = startTime.getMillis();
                                facet.end = facet.start + duration*1000;
                                facet.autodetected = misfitJson.getBoolean("autoDetected");
                                facet.sleepDetails = misfitJson.getString("sleepDetails");
                                return facet;
                            }
                        }, updateInfo.apiKey.getId());
        return ret;
    }

    private JSONArray getMisfitData(JSONObject jsonApiData, String dataTypeName) {
        Object o = jsonApiData.get(dataTypeName);
        if (o==null) throw new RuntimeException("Unexpected API format: no \"" + dataTypeName + "\" field in JSON");
        if (o instanceof JSONArray) return (JSONArray) o;
        else if (o instanceof JSONObject) {
            JSONArray array = new JSONArray();
            array.add(o);
            return array;
        }
        return new JSONArray();
    }

    private String getBackfillEndDate(UpdateInfo updateInfo, ObjectType objectType) {
        String backfillEndDate = guestService.getApiKeyAttribute(updateInfo.apiKey, BACKFILL_ENDDATE_ATTKEY_PREFIX + objectType.name());
        if (backfillEndDate!=null)
            return backfillEndDate;
        String endDate = getMaxEndDate();
        return endDate;
    }

    private String getMaxEndDate() {
        return ISODateTimeFormat.date().print(System.currentTimeMillis()+ DateTimeConstants.MILLIS_PER_DAY);
    }

    private boolean isTrue(String attValue) {
        return attValue!=null && attValue.equalsIgnoreCase("true");
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {
        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#fff";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#fff";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
        channelStyle.timespanStyles.defaultStyle.top = 0.0;
        channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
        channelStyle.timespanStyles.values = new HashMap<String, BodyTrackHelper.TimespanStyle>();

        BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = .0;
        stylePart.bottom = 0.9;
        stylePart.fillColor = "#77518b";
        stylePart.borderColor = "#77518b";
        channelStyle.timespanStyles.values.put("deep",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = .0;
        stylePart.bottom = 0.6;
        stylePart.fillColor = "#916ba5";
        stylePart.borderColor = "#916ba5";
        channelStyle.timespanStyles.values.put("light",stylePart);

        stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = .0;
        stylePart.bottom = 0.1;
        stylePart.fillColor = "#e1d4e6";
        stylePart.borderColor = "#e1d4e6";
        channelStyle.timespanStyles.values.put("wake",stylePart);

        bodytrackHelper.setBuiltinDefaultStyle(apiKey.getGuestId(), apiKey.getConnector().getName(), "sleep", channelStyle);
    }


    private final String makeRestCall(final UpdateInfo updateInfo,
                                      final int objectTypes, final String urlString, final String...method)
            throws RateLimitReachedException, UpdateFailedException, AuthExpiredException, UnexpectedResponseCodeException {
        // if have already called the API from within this thread, the allowed remaining API calls will be saved
        // in the updateInfo
        final Integer remainingAPICalls = updateInfo.getRemainingAPICalls("misfit");
        if (remainingAPICalls==null) {
            // otherwise, it means this is the first time we are calling the API
            // from within this update. It is possible that a previous update has consumed the entire API quota.
            // In this case, it has saved Misfit's reset time as an ApiKey attribute. If however, this is the
            // very first API call for this connector, we are most probably not rate limited and so we can continue.
            String apiKeyAttResetTime = guestService.getApiKeyAttribute(updateInfo.apiKey, "resetTime");
            if (apiKeyAttResetTime!=null) {
                long resetTime = Long.valueOf(apiKeyAttResetTime);
                if (resetTime>System.currentTimeMillis()) {
                    // reset updateInfo's reset time to this stored value so we don't delay next update to a default amount
                    updateInfo.setResetTime("misfit", resetTime);
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

            request.setDoInput(true);
            request.setDoOutput(true);
            request.setUseCaches(false);
            request.setRequestProperty("access_token", guestService.getApiKeyAttribute(updateInfo.apiKey, "accessToken"));

            request.connect();
            final int httpResponseCode = request.getResponseCode();

            final String httpResponseMessage = request.getResponseMessage();

            // retrieve and save rate limiting metadata
            final String remainingCalls = request.getHeaderField("X-RateLimit-Remaining");
            if (remainingCalls!=null) {
                updateInfo.setRemainingAPICalls("misfit", Integer.valueOf(remainingCalls));
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
                // Check for response code 429 which is Misfit's over rate limit error
                if(httpResponseCode == 429) {
                    logger.warn("Darn, we hit Misfit's rate limit again! url=" + urlString + ", guest=" + updateInfo.getGuestId());
                    // try to retrieve the reset time from Misfit, otherwise default to a one hour delay
                    // also, set resetTime as an apiKey attribute so we don't retry calling the API too soon
                    setResetTime(updateInfo, request);
                    throw new RateLimitReachedException();
                }
                else {
                    if (httpResponseCode == 401)
                        throw new AuthExpiredException();
                    else if (httpResponseCode >= 400 && httpResponseCode < 500) {
                        String message = "Unexpected response code: " + httpResponseCode;
                        throw new UpdateFailedException(message, true,
                                ApiKey.PermanentFailReason.clientError(httpResponseCode));
                    }
                    String reason = "Error: " + httpResponseCode;
                    throw new UpdateFailedException(false, reason);
                }
            }
        } catch (IOException exc) {
            throw new RuntimeException("IOException trying to make rest call: " + exc.getMessage());
        }
    }

    private void setResetTime(UpdateInfo updateInfo, HttpURLConnection request) {
        final String rateLimitResetSeconds = request.getHeaderField("X-RateLimit-Reset");
        if (rateLimitResetSeconds!=null) {
            final long resetTime = Long.valueOf(rateLimitResetSeconds)*1000;
            guestService.setApiKeyAttribute(updateInfo.apiKey, "resetTime", String.valueOf(resetTime));
            updateInfo.setResetTime("misfit", resetTime);
        } else {
            final long resetTime = System.currentTimeMillis() + 60 * DateTimeConstants.MILLIS_PER_HOUR;
            guestService.setApiKeyAttribute(updateInfo.apiKey, "resetTime", String.valueOf(resetTime));
            updateInfo.setResetTime("misfit", resetTime);
        }
    }

}
