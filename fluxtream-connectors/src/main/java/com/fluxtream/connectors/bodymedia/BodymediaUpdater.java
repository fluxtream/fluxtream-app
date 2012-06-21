package com.fluxtream.connectors.bodymedia;

import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.SignpostOAuthHelper;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.ConnectorUpdateService;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "BodyMedia", value = 88, objectTypes = {
        BodymediaBurnFacet.class, BodymediaSleepFacet.class,
        BodymediaStepsFacet.class}, hasFacets = true, additionalParameters = {"api_key"})
public class BodymediaUpdater extends AbstractUpdater
{

    @Autowired
    SignpostOAuthHelper signpostHelper;

    @Autowired
    ConnectorUpdateService connectorUpdateService;

    public BodymediaUpdater()
    {
        super();
    }

    public void updateConnectorDataHistory(UpdateInfo updateInfo)
            throws Exception
    {
        setupConsumer(updateInfo.apiKey);
        String api_key = env.get("bodymediaConsumerKey");

        ObjectType burnOT = ObjectType.getObjectType(connector(), "burn");
        String userRegistrationDate = getUserRegistrationDate(updateInfo, api_key);
        if (updateInfo.objectTypes().contains(burnOT))
        {
            //DateTime should be initialized to today
            DateTime today = new DateTime();
            DateTime start = DateTimeFormat.forPattern("yyyyMMdd").parseDateTime(userRegistrationDate);
            retrieveBurnHistory(updateInfo, start, today);
        }
    }

    /**
     * Retrieves that burn history from the start date to the end date. It peforms the api calls in reverse order
     * starting from the end date. This is so that the most recent information is retrieved first.
     * @param updateInfo The api's info
     * @param start The earliest date for which the burn history is retrieved. This date is included in the update.
     * @param end The latest date for which the burn history is retrieved. This date is also included in the update.
     * @throws Exception If either storing the data fails or if the rate limit is reached on Bodymedia's api
     */
    private void retrieveBurnHistory(UpdateInfo updateInfo, DateTime start, DateTime end) throws Exception
    {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
        DateTimeComparator comparator = DateTimeComparator.getDateOnlyInstance();
        DateTime current = end;
        while (comparator.compare(current, start) >= 0)
        //@ loop_invariant date.compareTo(userRegistrationDate) >= 0;
        {
            ObjectType burnOT = ObjectType.getObjectType(connector(), "burn");
            String burnMinutesUrl = "http://api.bodymedia.com/v2/json/burn/day/minute/intensity/" + current.toString(formatter) +
                                    "?api_key=" + updateInfo.apiKey.getAttributeValue("api_key", env);
            //The following call may fail due to bodymedia's api. That is expected behavior
            String jsonResponse = signpostHelper.makeRestCall(connector(), updateInfo.apiKey, burnOT.value(), burnMinutesUrl);
            apiDataService.cacheApiDataJSON(updateInfo, jsonResponse, -1, -1);
            current = current.minusDays(1);
        }
    }

    OAuthConsumer consumer;

    void setupConsumer(ApiKey apiKey)
    {
        String api_key = env.get("bodymediaConsumerKey");
        String bodymediaConsumerSecret = env.get("bodymediaConsumerSecret");

        consumer = new CommonsHttpOAuthConsumer(
                api_key,
                bodymediaConsumerSecret);

        String accessToken = apiKey.getAttributeValue("accessToken", env);
        String tokenSecret = apiKey.getAttributeValue("tokenSecret", env);

        consumer.setTokenWithSecret(accessToken,
                                    tokenSecret);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception
    {
        ObjectType burnOT = ObjectType.getObjectType(connector(), "burn");
        if(updateInfo.objectTypes().contains(burnOT))
        {
            DateTime today = new DateTime();
            DateTime start = getLastSyncTime(updateInfo, burnOT);
            retrieveBurnHistory(updateInfo, start, today);
        }
    }

    /**
     * Retrieves the User's lastSync time
     * @param updateInfo Used to identify the user
     * @return a DateTime that represents when the user last synced his device
     */
    private DateTime getLastSyncTime(final UpdateInfo updateInfo, ObjectType ot)
    {
        ApiUpdate a = connectorUpdateService.getLastSuccessfulUpdate(updateInfo.getGuestId(), connector(), ot.value());
        return new DateTime(a.lastSync);
    }

    public String getUserRegistrationDate(UpdateInfo updateInfo, String api_key)
            throws Exception
    {
        long then = System.currentTimeMillis();
        String requestUrl = "http://api.bodymedia.com/v2/json/user/info?api_key=" + api_key;

        HttpGet request = new HttpGet(requestUrl);
        consumer.sign(request);
        HttpClient client = env.getHttpClient();
        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200)
        {
            countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
                                   updateInfo.objectTypes, then, requestUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String json = responseHandler.handleResponse(response);
            JSONObject userInfo = JSONObject.fromObject(json);
            return userInfo.getString("registrationDate");
        }
        else
        {
            countFailedApiCall(updateInfo.apiKey.getGuestId(),
                               updateInfo.objectTypes, then, requestUrl);
            throw new Exception("Unexpected error trying to get statuses");
        }
    }
}
