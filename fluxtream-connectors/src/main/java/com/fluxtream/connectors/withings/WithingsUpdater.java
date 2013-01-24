package com.fluxtream.connectors.withings;

import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.utils.Utils;
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
        request.addQuerystringParameter("startdate", String.valueOf(since));
        request.addQuerystringParameter("publickey", env.get("withings.publickey"));
        request.addQuerystringParameter("enddate", String.valueOf(System.currentTimeMillis() / 1000));

        Token accessToken = new Token(updateInfo.apiKey.getAttributeValue("accessToken", env), updateInfo.apiKey.getAttributeValue("tokenSecret", env));

        try {
            withingsOAuthController.getOAuthService().signRequest(accessToken, request);

            Response response = request.send();
            if (response.getCode() == 200) {
                countSuccessfulApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then, url);
                json = response.getBody();
            }
            else {
                throw new Exception();
            }
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then, url, Utils.stackTrace(e));
            throw e;
        }
        apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(updateInfo.apiKey.getGuestId(),
						connector());
        fetchWithingsData(updateInfo, lastSuccessfulUpdate.ts / 1000);
	}

}
