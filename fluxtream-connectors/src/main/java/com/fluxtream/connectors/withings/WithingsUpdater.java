package com.fluxtream.connectors.withings;

import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.domain.Notification;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Withings", value = 4, objectTypes = {
        WithingsBPMMeasureFacet.class, WithingsBodyScaleMeasureFacet.class },
         extractor = WithingsFacetExtractor.class,
         defaultChannels = {"Withings.weight","Withings.systolic", "Withings.diastolic", "Withings.heartPulse"})
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

    Logger logger = Logger.getLogger(WithingsUpdater.class);

    @Autowired
    WithingsOAuthController withingsOAuthController;

    public WithingsUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        fetchWithingsData(updateInfo, 0);
    }

    private void fetchWithingsData(final UpdateInfo updateInfo, long since) throws Exception {
        long then = System.currentTimeMillis();
        String json;

        final String url = "http://wbsapi.withings.net/measure?action=getmeas";
        OAuthRequest request = new OAuthRequest(Verb.GET, url);
        request.addQuerystringParameter("userid", updateInfo.apiKey.getAttributeValue("userid", env));
        request.addQuerystringParameter("devtype", String.valueOf(5));
        request.addQuerystringParameter("startdate", String.valueOf(since));
        request.addQuerystringParameter("publickey", env.get("withings.publickey"));
        request.addQuerystringParameter("enddate", String.valueOf(System.currentTimeMillis() / 1000));

        final String accessTokenStr = updateInfo.apiKey.getAttributeValue("accessToken", env);
        final String tokenSecretStr = updateInfo.apiKey.getAttributeValue("tokenSecret", env);
        if(accessTokenStr == null || tokenSecretStr==null) {
            // This connector does not have the accessToken and/or tokenSecret attributes, which means that
            // the connector needs to be removed and re-added.
            String message = (new StringBuilder("<p>Your Withings connector needs to be reauthorized.  "))
                                .append("Please remove in Manage Connectors, then add the connector again.</p>")
                                .append("<p>We apologize for the inconvenience</p>").toString();
            notificationsService.addNotification(updateInfo.getGuestId(), Notification.Type.ERROR, message);
            return;
        }

        Token accessToken = new Token(accessTokenStr, tokenSecretStr);

        try {
            withingsOAuthController.getOAuthService().signRequest(accessToken, request);

            Response response = request.send();
            if (response.getCode() == 200) {
                countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
                json = response.getBody();
                JSONObject jsonObject = JSONObject.fromObject(json);
                final int status = jsonObject.getInt("status");
                if (status !=0) {
                    throw new Exception("bad status");
                }
            }
            else {
                throw new Exception();
            }
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url, Utils.stackTrace(e));
            throw e;
        }
        apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        ApiUpdate lastSuccessfulUpdate = connectorUpdateService
                .getLastSuccessfulUpdate(updateInfo.apiKey);
        fetchWithingsData(updateInfo, lastSuccessfulUpdate.ts / 1000);
    }

}
