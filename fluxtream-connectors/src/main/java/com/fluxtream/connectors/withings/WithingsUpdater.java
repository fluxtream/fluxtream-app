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
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.HttpUtils.fetch;

@Component
@Updater(prettyName = "Withings", value = 4, objectTypes = {
        WithingsBPMMeasureFacet.class, WithingsBodyScaleMeasureFacet.class, WithingsHeartPulseMeasureFacet.class,
            WithingsActivityFacet.class},
         extractor = WithingsFacetExtractor.class,
         defaultChannels = {"Withings.weight","Withings.systolic", "Withings.diastolic"})
@JsonFacetCollection(WithingsFacetVOCollection.class)
public class WithingsUpdater extends AbstractUpdater {

    private static final String LAST_ACTIVITY_SYNC_DATE = "lastActivitySyncDate";

    @Autowired
    JPADaoService jpaDaoService;

    protected transient DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    public WithingsUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        // get user info and find out first seen date
        long then = System.currentTimeMillis();
        String json = "";

        final String userid = guestService.getApiKeyAttribute(updateInfo.apiKey, "userid");
        final String publickey = guestService.getApiKeyAttribute(updateInfo.apiKey, "publickey");

        // do v1 API call
        String url = String.format("http://wbsapi.withings.net/measure?action=getmeas&userid=%s&publickey=%s&startdate=0&enddate=%s",
                                   userid, publickey,
                                   String.valueOf(System.currentTimeMillis() / 1000));
        fetchAndProcessJSON(updateInfo, url);

        // do v2 (activity) API call
        final String todaysDate = dateFormatter.withZoneUTC().print(System.currentTimeMillis());
        String urlv2 = String.format("http://wbsapi.withings.net/v2/measure?action=getactivity&userid=%s&startdateymd=2013-06-01&enddateymd=%s",
                                   userid, publickey, todaysDate);
        guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE, todaysDate);
        fetchAndProcessJSON(updateInfo, urlv2);
    }

    private void fetchAndProcessJSON(final UpdateInfo updateInfo, final String url) throws Exception {
        long then = System.currentTimeMillis();
        try {
            String json = fetch(url);
            JSONObject jsonObject = JSONObject.fromObject(json);
            if (jsonObject.getInt("status")!=0)
                throw new UnexpectedHttpResponseCodeException(jsonObject.getInt("status"), "Unexpected status code: " + jsonObject.getInt("status"));
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, url);
            if (StringUtils.isEmpty(json))
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

    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        long lastBodyscaleMeasurement = getLastBodyScaleMeasurement(updateInfo);
        long lastBloodPressureMeasurement = getLastBloodPressureMeasurement(updateInfo);

        long lastMeasurement = Math.max(lastBodyscaleMeasurement, lastBloodPressureMeasurement);

        final String userid = guestService.getApiKeyAttribute(updateInfo.apiKey, "userid");
        final String publickey = guestService.getApiKeyAttribute(updateInfo.apiKey, "publickey");
        final long startdate = lastMeasurement / 1000;
        final long enddate = System.currentTimeMillis() / 1000;

        // do v1 API call
        String url = String.format("http://wbsapi.withings.net/measure?action=getmeas&userid=%s&publickey=%s&startdate=%s&enddate=%s",
                                   userid, publickey, String.valueOf(startdate), String.valueOf(enddate));
        fetchAndProcessJSON(updateInfo, url);

        // do v2 API call
        final String todaysDate = dateFormatter.withZoneUTC().print(System.currentTimeMillis());
        final String lastActivitySyncDate = guestService.getApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE);
        String urlv2 = String.format("http://wbsapi.withings.net/v2/measure?action=getactivity&userid=%s&startdateymd=%s&enddateymd=%s",
                                     userid, publickey,
                                     lastActivitySyncDate,
                                     todaysDate);
        guestService.setApiKeyAttribute(updateInfo.apiKey, LAST_ACTIVITY_SYNC_DATE, todaysDate);
        fetchAndProcessJSON(updateInfo, urlv2);
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
