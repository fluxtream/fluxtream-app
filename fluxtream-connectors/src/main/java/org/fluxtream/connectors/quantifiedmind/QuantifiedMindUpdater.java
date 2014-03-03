package org.fluxtream.connectors.quantifiedmind;

import java.io.IOException;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.services.GuestService;
import org.fluxtream.utils.HttpUtils;
import org.fluxtream.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "QuantifiedMind", value = 100, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {QuantifiedMindTestFacet.class}, extractor = QuantifiedMindTestFacetExtractor.class)
public class QuantifiedMindUpdater extends AbstractUpdater {

    @Autowired
    GuestService guestService;

    public QuantifiedMindUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        // Don't do anything special for initial history update; updateStartMillis will default to 0
        updateConnectorData(updateInfo);
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        // Get start time from the stored attributes and update through now
        long updateStartMillis = getUpdateStartMillis(updateInfo);
        loadHistory(updateInfo, updateStartMillis, System.currentTimeMillis());
    }

    // Retrieve the stored value of when to start update from
    long getUpdateStartMillis(UpdateInfo updateInfo) {
        String updateKeyName = "updateStartMillis";
        String updateStartMillisAttrib = guestService.getApiKeyAttribute(updateInfo.apiKey, updateKeyName);

        // The first time we do this there won't be an apiKeyAttribute yet.  In that case default to 0
        // Otherwise parse it out of the attribute
        if(updateStartMillisAttrib != null) {
            // There is a stored update start time, parse it
            try {
                long updateStartMillis = Long.valueOf(updateStartMillisAttrib);
                return(updateStartMillis);
            }
            catch (Throwable e) {
                // Don't worry about parse errors, just start over at 0
            }
        }
        return 0;
    }


    // Update the current progress on the update so we can know where to start next time.
    void setUpdateStartMillis(UpdateInfo updateInfo, long updateProgressMillis)
    {
        String updateKeyName = "updateStartMillis";
        guestService.setApiKeyAttribute(updateInfo.apiKey, updateKeyName, String.valueOf(updateProgressMillis));
    }

    // Get and update data between the start and end times specified, updating updateStartMillis as we go
    private void loadHistory(UpdateInfo updateInfo, long from, long to) throws Exception {
        String queryUrl = "request url not set yet";
        long then = System.currentTimeMillis();
        String username = guestService.getApiKeyAttribute(updateInfo.apiKey,
                                                          "username");
        String token = guestService.getApiKeyAttribute(updateInfo.apiKey,
                                                       "token");
        try {
            boolean partialResult = false;
            // Start at the from time specified by the caller, and handle additional pages if we need to,
            // updating updateStartMillis each time we're successful.  from and to are in units of long milliseconds
            // and start_time and next_date are in units of double seconds.  Do the conversion.
            String start_time = String.valueOf(from/1000.0);
            do {
                queryUrl = "http://www.quantified-mind.com/api/get_session_data?username=" + username + "&token=" + token;
                queryUrl += "&start_time=" + start_time;

                final String json = HttpUtils.fetch(queryUrl);
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, queryUrl);

                JSONObject jsonObject = JSONObject.fromObject(json);
                String status = jsonObject.getString("status");
                partialResult = status.equals("partial");
                JSONArray sessionData = jsonObject.getJSONArray("session_data");
                final String sessionDataJSON = sessionData.toString();
                apiDataService.cacheApiDataJSON(updateInfo, sessionDataJSON, -1, -1);
                if (partialResult) {
                    // If we got a partial result, save next_date as the updateStartMillis
                    // to start from next time
                    start_time = jsonObject.getString("next_date");
                    // next_date is floating point; parse as a Double then convert to long
                    setUpdateStartMillis(updateInfo, (long)Math.floor(Double.valueOf(start_time)*1000.0));
                }
            } while (partialResult);
        }
        catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e), e.getHttpResponseCode(), e.getHttpResponseMessage());
            throw new Exception("Could not get QuantifiedMind tests: "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }
        catch (IOException e) {
            reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e), "I/O");
            throw new Exception("Unexpected error, getting QuantifiedMind tests: "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }

        // We completed successfully and processed all data until the present.  Store the value of "to",
        // which is the time we started the update, as the time to start from next time in order to
        // be conservative about not missing items which appeared between the start of the update and now.
        setUpdateStartMillis(updateInfo, Long.valueOf(to));
    }

}