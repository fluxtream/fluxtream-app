package org.fluxtream.connectors.mymee;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.Tag;
import org.fluxtream.core.services.ApiDataService.FacetModifier;
import org.fluxtream.core.services.ApiDataService.FacetQuery;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.HttpUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
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

    static FlxLogger logger = FlxLogger.getLogger(MymeeUpdater.class);

    @Autowired
    GuestService guestService;

    protected static DateTimeFormatter iso8601Formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeZone UTC = DateTimeZone.forID("UTC");

    @Autowired
    BodyTrackHelper bodytrackHelper;

    @Autowired
    MetadataService metadataService;


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
        // Reset last_seq so that incremental update will pull everything
        guestService.setApiKeyAttribute(updateInfo.apiKey, "last_seq","0");
        // Flush all of the facets for this connector to the datastore
        updateConnectorData(updateInfo);
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        String rootURL = getRootURL(updateInfo);
        long lastSeq = 0;
        try {
            lastSeq = Long.valueOf(guestService.getApiKeyAttribute(updateInfo.apiKey, "last_seq"));
        } catch (Exception e) {}

        // Fetch and load changes, starting with lastSeq, fetching at most maxToFetch each pass
        final int maxToFetch = 100;

        Set<String> channelNames = new HashSet<String>();

        while (true) {
            String URL = rootURL + "/_changes?since=" + lastSeq + "&limit=" + maxToFetch + "&include_docs=true";
            long newLastSeq;
            JSONArray changes;

            try {
                JSONObject json = JSONObject.fromObject(HttpUtils.fetch(URL));
                newLastSeq = json.getLong("last_seq");
                changes = json.getJSONArray("results");
            }
            catch (UnexpectedHttpResponseCodeException e) {
                countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, System.currentTimeMillis(), URL,
                                   Utils.stackTrace(e), e.getHttpResponseCode(), e.getHttpResponseMessage());
                throw new Exception("Could not get Mymee observations: "
                                    + e.getMessage() + "\n" + Utils.stackTrace(e));
            } catch (IOException e) {
                reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, System.currentTimeMillis(), URL,
                                    Utils.stackTrace(e), "I/O");
                throw new Exception("Could not get Mymee observations: "
                                    + e.getMessage() + "\n" + Utils.stackTrace(e));
            }
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes,
                                   System.currentTimeMillis(), URL);

            // If last_seq is the same as we passed, there are no more observations
            if (newLastSeq == lastSeq) {
                break;
            }

            //logger.info("MymeeUpdater got changes: " + changes.toString());

            // Loop over changes
            List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();
            for (int i = 0; i < changes.size(); i++) {
                JSONObject change = changes.getJSONObject(i);
                // Skip deleted objects.  Someday, we might consider deleting them here
                if (change.optBoolean("deleted", false)) {
                    continue;
                }
                JSONObject observation = change.getJSONObject("doc");
                if (observation.optString("type").equals("observation")) {
                    MymeeObservationFacet newFacet = createOrUpdateObservation(updateInfo, rootURL, observation);
                    if(newFacet!=null) {
                        newFacets.add(newFacet);

                        // Channel names have all characters that aren't alphanumeric or underscores replaced with underscores
                        channelNames.add(newFacet.getChannelName());

                    }
                }
            }


            // Write the new set of observations into the datastore
            bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), newFacets);

            lastSeq = newLastSeq;

            // Write lastSeq back to apiKeyAttributes
            guestService.setApiKeyAttribute(updateInfo.apiKey, "last_seq", String.valueOf(lastSeq));
        }

        // For each Mymee channel, setup the default display style to be lollipops
        for (String channelName : channelNames) {
            bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "Mymee", channelName, lollipopStyle);
        }

    }

    // Parses observation and loads it into the database
    // If database already has an observation with matching ID, update that observation.  Otherwise,
    // insert as a new observation
    // If malformed, logs an error and returns null rather than throwing an exception.
    // Note that this function does not load values into the datastore

    private MymeeObservationFacet createOrUpdateObservation(final UpdateInfo updateInfo, final String rootURL, final JSONObject observation) {
        try {
            // mymeeId is unique for each observation
            final String mymeeId = observation.getString("_id");

            MymeeObservationFacet ret = (MymeeObservationFacet)
                    apiDataService.createOrReadModifyWrite(MymeeObservationFacet.class,
                                                           new FacetQuery(
                                                                   "e.apiKeyId = ? AND e.mymeeId = ?",
                                                                   updateInfo.apiKey.getId(),
                                                                   mymeeId),
                                                           new FacetModifier<MymeeObservationFacet>() {
                        // Throw exception if it turns out we can't make sense of the observation's JSON
                        // This will abort the transaction
                        @Override
                        public MymeeObservationFacet createOrModify(MymeeObservationFacet facet, Long apiKeyId) {
                            if (facet == null) {
                                facet = new MymeeObservationFacet(updateInfo.apiKey.getId());
                                facet.mymeeId = mymeeId;
                                // auto-populate the facet's tags field with the name of the observation (e.g. "Food", "Back Pain", etc.)
                                facet.addTags(Tag.cleanse(facet.name), Tag.SPACE_DELIMITER);
                                facet.guestId = updateInfo.apiKey.getGuestId();
                                facet.api = updateInfo.apiKey.getConnector().value();
                            }

                            facet.name = observation.getString("name");

                            facet.timeUpdated = System.currentTimeMillis();

                            final DateTime happened = iso8601Formatter.withZone(UTC)
                                    .parseDateTime(observation.getString("happened"));
                            facet.start = facet.end = happened.getMillis();

                            try {
                                facet.timezoneOffset = observation.getInt("timezoneOffset");
                            } catch (Throwable ignored) {
                                facet.timezoneOffset = null;
                            }

                            facet.note = observation.optString("note", null);
                            // Store the note in the comment field if comment not already set (e.g. for photos)
                            if (facet.comment == null) {
                                facet.comment = facet.note;
                            }

                            facet.user = observation.optString("user", null);
                            facet.unit = observation.optString("unit", null);
                            facet.baseUnit = observation.optString("baseunit", null);


                            try {
                                facet.amount = observation.getDouble("amount");
                            } catch (Throwable ignored) {
                                facet.amount = null;
                            }

                            try {
                                facet.baseAmount = observation.getInt("baseAmount");
                            } catch (Throwable ignored) {
                                facet.baseAmount = null;
                            }

                            try {
                                JSONArray locArray = observation.getJSONArray("loc");
                                facet.longitude = locArray.getDouble(0);
                                facet.latitude = locArray.getDouble(1);

                                if(facet.longitude!=null && facet.latitude!=null) {
                                    // Create a location for updating visited cities list
                                    LocationFacet locationFacet = new LocationFacet(updateInfo.apiKey.getId());
                                    locationFacet.guestId = updateInfo.getGuestId();
                                    locationFacet.source = LocationFacet.Source.MYMEE;
                                    locationFacet.api = updateInfo.apiKey.getConnector().value();
                                    locationFacet.start = locationFacet.end = locationFacet.timestampMs = facet.start;
                                    locationFacet.latitude = facet.latitude.floatValue();
                                    locationFacet.longitude = facet.longitude.floatValue();

                                     // Process the location facet into visited cities
                                    List<LocationFacet> locationFacets = new ArrayList<LocationFacet>();
                                    locationFacets.add(locationFacet);
                                    metadataService.updateLocationMetadata(updateInfo.getGuestId(), locationFacets);
                                }
                            } catch (Throwable ignored) {
                                facet.longitude = facet.latitude = null;
                            }

                            try {
                                // If there's an attachment, we assume there's only one and that it's an image
                                final JSONObject imageAttachment = observation.getJSONObject("_attachments");
                                final String imageName = (String) imageAttachment.names().get(0);
                                facet.imageURL = rootURL + "/" + facet.mymeeId + "/" + imageName;
                            } catch (Throwable ignored) {
                                facet.imageURL = null;
                            }

                            //System.out.println("====== mymeeId=" + facet.mymeeId + ", timeUpdated=" + facet.timeUpdated);
                            return facet;
                        }
                    }, updateInfo.apiKey.getId());
            return ret;

        } catch (Throwable e) {
            // Couldn't makes sense of observation's JSON
            return null;
        }
    }

    // Returns root URL for mymee database, without trailing / (e.g. http://hostname/databasename)
    private String getRootURL(final UpdateInfo updateInfo) {
        final String fetchURL = guestService.getApiKeyAttribute(updateInfo.apiKey,"fetchURL");
        return getBaseURL(fetchURL) +  "/" + getMainDir(fetchURL);
    }


    private void loadEverything(final UpdateInfo updateInfo, boolean incremental) throws Exception {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=loadEverythingFacet")
                .append(" connector=")
                .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                .append(updateInfo.apiKey.getGuestId());
        logger.info(sb.toString());
        long then = System.currentTimeMillis();
        String queryUrl = guestService.getApiKeyAttribute(updateInfo.apiKey, "fetchURL");
        String json = null;
        try {
            json = HttpUtils.fetch(queryUrl);
        }
        catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e),
                               e.getHttpResponseCode(), e.getHttpResponseMessage());
        } catch (IOException e) {
            reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then,
                               queryUrl, Utils.stackTrace(e), "I/O");
            throw new Exception("Could not get Mymee observations: "
                                + e.getMessage() + "\n" + Utils.stackTrace(e));
        }

        countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, queryUrl);

        if (json!=null) {
            Set<String> channelNames = extractFacets(json, updateInfo, incremental);
            for (String channelName : channelNames) {
                bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "Mymee", channelName, lollipopStyle);
            }
        }
    }

    public Set<String> extractFacets(final String json, final UpdateInfo updateInfo, boolean incremental) throws Exception {
        StringBuilder sb = new StringBuilder("module=updateQueue component=updater action=extractFacet")
                .append(" connector=")
                .append(updateInfo.apiKey.getConnector().toString()).append(" guestId=")
                .append(updateInfo.apiKey.getGuestId());
        logger.info(sb.toString());
        JSONObject mymeeData = JSONObject.fromObject(json);
        JSONArray array = mymeeData.getJSONArray("rows");
        Set<String> channelNames = new HashSet<String>();
        List<AbstractFacet> newFacets = new ArrayList<AbstractFacet>();

        for(int i=0; i<array.size(); i++) {
            MymeeObservationFacet facet = new MymeeObservationFacet(updateInfo.apiKey.getId());

            JSONObject observationObject = array.getJSONObject(i);

            JSONObject valueObject = observationObject.getJSONObject("value");

            if (valueObject == null) {
                continue;
            }

            int timezoneOffset = valueObject.getInt("timezoneOffset");
            facet.guestId = updateInfo.apiKey.getGuestId();
            facet.api = updateInfo.apiKey.getConnector().value();
            facet.timeUpdated = System.currentTimeMillis();

            final DateTime happened = iso8601Formatter.withZone(UTC)
                    .parseDateTime(valueObject.getString("happened"));
            facet.start = happened.getMillis();
            facet.end = facet.start;

            facet.timezoneOffset = timezoneOffset;
            facet.mymeeId = observationObject.getString("id");

            // ignore facet if we already have it in the database
            // do that only if incrementally updating
            if (incremental) {
                final List<MymeeObservationFacet> observationFacets = jpaDaoService.find("mymee.observation.byMymeeId", MymeeObservationFacet.class, updateInfo.getGuestId(), facet.mymeeId);
                if (observationFacets!=null && observationFacets.size()>0) {
                    continue;
                }
            }

            facet.name = valueObject.getString("name");

            // Channel names have all characters that aren't alphanumeric or underscores replaced with underscores
            channelNames.add(facet.name.replaceAll("[^0-9a-zA-Z_]+", "_"));

            // auto-populate the facet's tags field with the name of the observation (e.g. "Food", "Back Pain", etc.)
            facet.addTags(Tag.cleanse(facet.name), Tag.SPACE_DELIMITER);

            if (valueObject.has("note")) {
                facet.note = valueObject.getString("note");
                facet.comment = facet.note; // also store the comment in the comment field (this is required for photos)
            }
            if (valueObject.has("user")) {
                facet.user = valueObject.getString("user");
            }

            if (valueObject.has("unit")) {
                facet.unit = valueObject.getString("unit");
            }
            if (valueObject.has("baseunit")) {
                facet.baseUnit = valueObject.getString("baseunit");
            }

            try{
                //if (valueObject.has("amount"))
                //    facet.amount = valueObject.getInt("amount");
            } catch (Throwable e){
            }

            try{
                //if (valueObject.has("baseAmount"))
                //    facet.baseAmount = valueObject.getInt("baseAmount");
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
                    final String fetchURL = guestService.getApiKeyAttribute(updateInfo.apiKey, "fetchURL");
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
            newFacets.add(facet);
        }
        if(incremental) {
            bodyTrackStorageService.storeApiData(updateInfo.apiKey.getGuestId(), newFacets);
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
            if (splits.length > 1) {
                return splits[1];
            }
        }
        catch (URISyntaxException e) {
            return null;
        }
        return null;
    }

}