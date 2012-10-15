package com.fluxtream.connectors.mymee;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.Tag;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Mymee", value = 110, updateStrategyType = Connector.UpdateStrategyType.INCREMENTAL,
         objectTypes = {MymeeObservationFacet.class}, extractor = MymeeObservationFacetExtractor.class)
public class MymeeUpdater extends AbstractUpdater {

    @Autowired
    GuestService guestService;

    protected static DateTimeFormatter iso8601Formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeZone timeZone = DateTimeZone.forID("UTC");

    @Autowired
    BodyTrackHelper bodytrackHelper;

    final String lollipopStyle = "{\"styles\":[{\"type\":\"line\",\"show\":false,\"lineWidth\":1}," +
                                 "{\"radius\":0,\"fill\":false,\"type\":\"lollipop\",\"show\":true,\"lineWidth\":1}," +
                                 "{\"radius\":2,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}," +
                                 "{\"marginWidth\":5,\"verticalOffset\":7," +
                                 "\"numberFormat\":\"###,##0\",\"type\":\"value\",\"show\":true}]," +
                                 "\"comments\":" +
                                 "{\"styles\":[{\"radius\":3,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}]," +
                                 "\"verticalMargin\":4,\"show\":true}}";

    public MymeeUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(final UpdateInfo updateInfo) throws Exception {
        apiDataService.eraseApiData(updateInfo.getGuestId(), connector());
        loadEverything(updateInfo);
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        loadEverything(updateInfo);
    }

    private void loadEverything(final UpdateInfo updateInfo) throws Exception {
        long then = System.currentTimeMillis();
        String queryUrl = guestService.getApiKeyAttribute(updateInfo.getGuestId(), connector(), "fetchURL");
        String json = null;
        try {
            json = HttpUtils.fetch(queryUrl);
        }
        catch (Exception e) {
            countFailedApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e));
            throw new Exception("Could not get Mymee observations: "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }

        countSuccessfulApiCall(updateInfo.apiKey.getGuestId(), updateInfo.objectTypes, then, queryUrl);

        if (json!=null) {
            Set<String> channelNames = extractFacets(json, updateInfo);
            for (String channelName : channelNames)
                bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "Mymee", channelName, lollipopStyle);
        }
    }

    public Set<String> extractFacets(final String json, final UpdateInfo updateInfo) throws Exception {
        JSONObject mymeeData = JSONObject.fromObject(json);
        JSONArray array = mymeeData.getJSONArray("rows");
        Set<String> channelNames = new HashSet<String>();
        for(int i=0; i<array.size(); i++) {
            MymeeObservationFacet facet = new MymeeObservationFacet();

            JSONObject observationObject = array.getJSONObject(i);

            JSONObject valueObject = observationObject.getJSONObject("value");

            if (valueObject==null)
                continue;

            int timezoneOffset = valueObject.getInt("timezoneOffset");
            facet.guestId = updateInfo.apiKey.getGuestId();
            facet.api = updateInfo.apiKey.getConnector().value();
            facet.timeUpdated = System.currentTimeMillis();

            final DateTime happened = iso8601Formatter.withZone(timeZone)
                    .parseDateTime(valueObject.getString("happened"));
            facet.start = happened.getMillis();
            facet.end = facet.start;

            facet.timezoneOffset = timezoneOffset;
            facet.mymeeId = observationObject.getString("id");

            // ignore facet if we already have it in the database
            final List<MymeeObservationFacet> observationFacets = jpaDaoService.find("mymee.observation.byMymeeId", MymeeObservationFacet.class, updateInfo.getGuestId(), facet.mymeeId);
            if (observationFacets!=null && observationFacets.size()>0)
                continue;

            facet.name = valueObject.getString("name");

            // Channel names have all characters that aren't alphanumeric or underscores replaced with underscores
            channelNames.add(facet.name.replaceAll("[^0-9a-zA-Z_]+", "_"));

            // auto-populate the facet's tags field with the name of the observation (e.g. "Food", "Back Pain", etc.)
            facet.addTags(Tag.cleanse(facet.name));

            if (valueObject.has("note")) {
                facet.note = valueObject.getString("note");
                facet.comment = facet.note; // also store the comment in the comment field (this is required for photos)
            }
            if (valueObject.has("user"))
                facet.user = valueObject.getString("user");

            if (valueObject.has("unit"))
                facet.unit = valueObject.getString("unit");
            if (valueObject.has("baseunit"))
                facet.baseUnit = valueObject.getString("baseunit");

            try{
                if (valueObject.has("amount"))
                    facet.amount = valueObject.getInt("amount");
            } catch (Throwable e){
            }

            try{
                if (valueObject.has("baseAmount"))
                    facet.baseAmount = valueObject.getInt("baseAmount");
            } catch (Throwable e){
            }

            try{
                if (valueObject.has("loc")) {
                    JSONArray locArray = valueObject.getJSONArray("loc");
                    facet.longitude = locArray.getDouble(0);
                    facet.latitude = locArray.getDouble(1);
                }
            } catch (Throwable e){
            }

            try {
                if (valueObject.has("_attachments")) {
                    // we assume that there's only one attachment and that it's an image
                    final JSONObject imageAttachment = valueObject.getJSONObject("_attachments");
                    final String imageName = (String) imageAttachment.names().get(0);
                    final String fetchURL = updateInfo.apiKey.getAttributeValue("fetchURL", env);
                    final String baseURL = getBaseURL(fetchURL);
                    final String mainDir = getMainDir(fetchURL);
                    if (baseURL!=null&&mainDir!=null) {
                        facet.imageURL = new StringBuilder(baseURL).append("/")
                                .append(mainDir).append("/").append(facet.mymeeId)
                                .append("/").append(imageName).toString();
                    }
                }
            } catch (Throwable e){
            }

            apiDataService.persistFacet(facet);
        }
        return channelNames;
    }

    public static String getBaseURL(String url) {
        try {
            URI uri = new URI(url);
            return (new StringBuilder(uri.getScheme()).append("://").append(uri.getHost()).toString());
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    public static String getMainDir(String url) {
        try {
            URI uri = new URI(url);
            final String[] splits = uri.getRawPath().split("/");
            if (splits.length>1)
                return splits[1];
        }
        catch (URISyntaxException e) {
            return null;
        }
        return null;
    }

}