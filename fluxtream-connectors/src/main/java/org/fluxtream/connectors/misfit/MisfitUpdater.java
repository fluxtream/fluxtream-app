package org.fluxtream.connectors.misfit;

import org.apache.commons.io.IOUtils;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Autonomous;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.*;
import org.fluxtream.core.domain.ApiKey;
import org.joda.time.DateTimeConstants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by candide on 09/02/15.
 */
@Component
@Updater(prettyName = "Misfit", value = 8, objectTypes = {MisfitActivitySummaryFacet.class, MisfitActivitySessionFacet.class, MisfitSleepFacet.class},
        userProfile = MisfitUserProfile.class
//        bodytrackResponder = MisfitBodytrackResponder.class,
//        defaultChannels = {"Misfit.steps"}
)
public class MisfitUpdater extends AbstractUpdater implements Autonomous {


    FlxLogger logger = FlxLogger.getLogger(MisfitUpdater.class);

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        updateConnectorData(updateInfo);
    }

    @Override
    protected void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        String json = makeRestCall(updateInfo, 1 + 2 + 4, "https://api.misfitwearables.com/move/resource/v1/user/me/activity/summary?start_date=1970-02-19&end_date=2015-02-20&detail=true");
        System.out.println(json);
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {

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
            int millisUntilReset = Integer.valueOf(rateLimitResetSeconds)*1000;
            // delay by one minute to compensate for clock desynchronisation
            final long resetTime = System.currentTimeMillis() + millisUntilReset + DateTimeConstants.MILLIS_PER_MINUTE;
            guestService.setApiKeyAttribute(updateInfo.apiKey, "resetTime", String.valueOf(resetTime));
            updateInfo.setResetTime("misfit", resetTime);
        } else {
            final long resetTime = System.currentTimeMillis() + 60 * DateTimeConstants.MILLIS_PER_HOUR;
            guestService.setApiKeyAttribute(updateInfo.apiKey, "resetTime", String.valueOf(resetTime));
            updateInfo.setResetTime("misfit", resetTime);
        }
    }

    private String getConsumerKey(final ApiKey apiKey) {
        return guestService.getApiKeyAttribute(apiKey, "misfitConsumerKey");
    }

    private String getConsumerSecret(final ApiKey apiKey) {
        return guestService.getApiKeyAttribute(apiKey, "misfitConsumerSecret");
    }

}
