package com.fluxtream.connectors.withings;

import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.utils.Utils;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.HttpUtils.fetch;

@Component
@Updater(prettyName = "Withings", value = 4, objectTypes = {
        WithingsBPMMeasureFacet.class, WithingsBodyScaleMeasureFacet.class },
         extractor = WithingsFacetExtractor.class,
         defaultChannels = {"Withings.weight","Withings.systolic", "Withings.diastolic", "Withings.heartPulse"})
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

    public WithingsUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        // get user info and find out first seen date
        long then = System.currentTimeMillis();
        String json;

        String url = "http://wbsapi.withings.net/measure?action=getmeas";
        url += "&userid="
               + updateInfo.apiKey.getAttributeValue("userid", env);
        url += "&publickey="
               + updateInfo.apiKey.getAttributeValue("publickey", env);
        url += "&startdate=0";
        url += "&enddate=" + System.currentTimeMillis() / 1000;

        try {
            json = fetch(url);
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, url);
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, url, Utils.stackTrace(e));
            throw e;
        }
        if (!json.equals(""))
            apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        long then = System.currentTimeMillis();
        String json;

        ApiUpdate lastSuccessfulUpdate = connectorUpdateService
                .getLastSuccessfulUpdate(updateInfo.apiKey);

        String url = "http://wbsapi.withings.net/measure?action=getmeas";
        url += "&userid=" + updateInfo.apiKey.getAttributeValue("userid", env);
        url += "&publickey="
               + updateInfo.apiKey.getAttributeValue("publickey", env);
        url += "&startdate=" + lastSuccessfulUpdate.ts / 1000;
        url += "&enddate=" + System.currentTimeMillis() / 1000;

        try {
            json = fetch(url);
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, url);
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, url, Utils.stackTrace(e));
            throw e;
        }
        apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
    }

}
