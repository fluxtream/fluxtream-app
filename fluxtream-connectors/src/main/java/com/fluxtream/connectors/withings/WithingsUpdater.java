package com.fluxtream.connectors.withings;

import java.io.IOException;
import java.util.List;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.HttpUtils.fetch;

@Component
@Updater(prettyName = "Withings", value = 4, objectTypes = {
        WithingsBPMMeasureFacet.class, WithingsBodyScaleMeasureFacet.class, WithingsHeartPulseMeasureFacet.class },
         extractor = WithingsFacetExtractor.class,
         defaultChannels = {"Withings.weight","Withings.systolic", "Withings.diastolic"})
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

    @Autowired
    JPADaoService jpaDaoService;

    public WithingsUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        // get user info and find out first seen date
        long then = System.currentTimeMillis();
        String json = "";

        String url = "http://wbsapi.withings.net/measure?action=getmeas";
        url += "&userid="
               + updateInfo.apiKey.getAttributeValue("userid", env);
        url += "&publickey="
               + updateInfo.apiKey.getAttributeValue("publickey", env);
        url += "&startdate=0";
        url += "&enddate=" + System.currentTimeMillis() / 1000;

        try {
            json = fetch(url);
            JSONObject jsonObject = JSONObject.fromObject(json);
            if (jsonObject.getInt("status")!=0)
                throw new Exception("Unexpected status code " + jsonObject.getInt("status"));
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, url);
        } catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, url, Utils.stackTrace(e),
                               e.getHttpResponseCode(), e.getHttpResponseMessage());
        } catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey,
                               updateInfo.objectTypes, then, url, Utils.stackTrace(e),
                               null, null);
            throw e;
        }
        if (!json.equals(""))
            apiDataService.cacheApiDataJSON(updateInfo, json, -1, -1);
    }

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        long then = System.currentTimeMillis();
        String json;

        long lastBodyscaleMeasurement = getLastBodyScaleMeasurement(updateInfo);
        long lastBloodPresseureMeasurement = getLastBloodPressureMeasurement(updateInfo);

        long lastMeasurement = Math.max(lastBodyscaleMeasurement, lastBloodPresseureMeasurement);

        String url = "http://wbsapi.withings.net/measure?action=getmeas";
        url += "&userid=" + updateInfo.apiKey.getAttributeValue("userid", env);
        url += "&publickey="
               + updateInfo.apiKey.getAttributeValue("publickey", env);
        url += "&startdate=" + lastMeasurement / 1000;
        url += "&enddate=" + System.currentTimeMillis() / 1000;

        try {
            json = fetch(url);
            JSONObject jsonObject = JSONObject.fromObject(json);
            if (jsonObject.getInt("status")!=0)
                throw new UnexpectedHttpResponseCodeException(jsonObject.getInt("status"), "Unexpected status code: " + jsonObject.getInt("status"));
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
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

    private long getLastBloodPressureMeasurement(final UpdateInfo updateInfo) {
        final String entityName = JPAUtils.getEntityName(WithingsBPMMeasureFacet.class);
        final List<WithingsBPMMeasureFacet> facets = jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC", 1, WithingsBPMMeasureFacet.class, updateInfo.apiKey.getId());
        if (facets.size()==0) return 0;
        return facets.get(0).start + 1000;
    }

    private long getLastBodyScaleMeasurement(final UpdateInfo updateInfo) {
        final String entityName = JPAUtils.getEntityName(WithingsBodyScaleMeasureFacet.class);
        final List<WithingsBodyScaleMeasureFacet> facets = jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.start DESC", 1, WithingsBodyScaleMeasureFacet.class, updateInfo.apiKey.getId());
        if (facets.size()==0) return 0;
        return facets.get(0).start + 1000;
    }
}
