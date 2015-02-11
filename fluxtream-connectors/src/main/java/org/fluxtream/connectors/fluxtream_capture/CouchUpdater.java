package org.fluxtream.connectors.fluxtream_capture;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.AbstractFacet;
import org.fluxtream.core.domain.Tag;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.BodyTrackStorageService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by candide on 11/02/15.
 */
@Component
public class CouchUpdater {

    @Autowired
    GuestService guestService;

    @Autowired
    BodyTrackHelper bodytrackHelper;

    @Autowired
    MetadataService metadataService;

    @Autowired
    BodyTrackStorageService bodyTrackStorageService;

    @Autowired
    ApiDataService apiDataService;

    final String lollipopStyle = "{\"styles\":[{\"type\":\"line\",\"show\":false,\"lineWidth\":1}," +
            "{\"radius\":0,\"fill\":false,\"type\":\"lollipop\",\"show\":true,\"lineWidth\":1}," +
            "{\"radius\":2,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}," +
            "{\"marginWidth\":5,\"verticalOffset\":7," +
            "\"numberFormat\":\"###,##0\",\"type\":\"value\",\"show\":true}]," +
            "\"comments\":" +
            "{\"styles\":[{\"radius\":3,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}]," +
            "\"verticalMargin\":4,\"show\":true}}";

    protected static DateTimeFormatter iso8601Formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeZone UTC = DateTimeZone.forID("UTC");

    public void updateCaptureData(UpdateInfo updateInfo, FluxtreamCaptureUpdater updater) throws Exception {
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
                JSONObject json = JSONObject.fromObject(fetchRetrying(URL, 20));
                newLastSeq = json.getLong("last_seq");
                changes = json.getJSONArray("results");
            }
            catch (UnexpectedHttpResponseCodeException e) {
                updater.countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, System.currentTimeMillis(), URL,
                        Utils.stackTrace(e), e.getHttpResponseCode(), e.getHttpResponseMessage());
                throw new Exception("Could not get Fluxtream observations: "
                        + e.getMessage() + "\n" + Utils.stackTrace(e));
            } catch (IOException e) {
                updater.reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, System.currentTimeMillis(), URL,
                        Utils.stackTrace(e), "I/O");
                throw new Exception("Could not get Fluxtream observations: "
                        + e.getMessage() + "\n" + Utils.stackTrace(e));
            }
            updater.countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes,
                    System.currentTimeMillis(), URL);

            // If last_seq is the same as we passed, there are no more observations
            if (newLastSeq == lastSeq) {
                break;
            }

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
                    FluxtreamObservationFacet newFacet = createOrUpdateObservation(updateInfo, rootURL, observation);
                    if(newFacet!=null) {
                        newFacets.add(newFacet);

                        // Channel names have all characters that aren't alphanumeric or underscores replaced with underscores
                        channelNames.add(newFacet.getChannelName());

                    }
                }
            }


            // Write the new set of observations into the datastore
            bodyTrackStorageService.storeApiData(updateInfo.apiKey, newFacets);

            lastSeq = newLastSeq;

            // Write lastSeq back to apiKeyAttributes
            guestService.setApiKeyAttribute(updateInfo.apiKey, "last_seq", String.valueOf(lastSeq));
        }

        // For each Fluxtream channel, setup the default display style to be lollipops
        for (String channelName : channelNames) {
            bodytrackHelper.setBuiltinDefaultStyle(updateInfo.getGuestId(), "Mymee", channelName, lollipopStyle);
        }
    }
    private FluxtreamObservationFacet createOrUpdateObservation(final UpdateInfo updateInfo, final String rootURL, final JSONObject observation) {
        try {
            // fluxtreamId is unique for each observation
            final String fluxtreamId = observation.getString("_id");

            FluxtreamObservationFacet ret =
                    apiDataService.createOrReadModifyWrite(FluxtreamObservationFacet.class,
                            new ApiDataService.FacetQuery(
                                    "e.apiKeyId = ? AND e.fluxtreamId = ?",
                                    updateInfo.apiKey.getId(),
                                    fluxtreamId),
                            new ApiDataService.FacetModifier<FluxtreamObservationFacet>() {
                                // Throw exception if it turns out we can't make sense of the observation's JSON
                                // This will abort the transaction
                                @Override
                                public FluxtreamObservationFacet createOrModify(FluxtreamObservationFacet facet, Long apiKeyId) {
                                    if (facet == null) {
                                        facet = new FluxtreamObservationFacet(updateInfo.apiKey.getId());
                                        facet.fluxtreamId = fluxtreamId;
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
                                        facet.imageURL = rootURL + "/" + facet.fluxtreamId + "/" + imageName;
                                    } catch (Throwable ignored) {
                                        facet.imageURL = null;
                                    }

                                    //System.out.println("====== fluxtreamId=" + facet.fluxtreamId + ", timeUpdated=" + facet.timeUpdated);
                                    return facet;
                                }
                            }, updateInfo.apiKey.getId());
            return ret;

        } catch (Throwable e) {
            // Couldn't makes sense of observation's JSON
            return null;
        }
    }
    // Returns root URL for fluxtream capture database, without trailing / (e.g. http://hostname/databasename)
    private String getRootURL(final UpdateInfo updateInfo) {
        final String fetchURL = guestService.getApiKeyAttribute(updateInfo.apiKey,"fetchURL");
        return getBaseURL(fetchURL) +  "/" + getMainDir(fetchURL);
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

    String fetchRetrying(final String url, final int retries) throws IOException, UnexpectedHttpResponseCodeException {
        HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                System.out.println(url + ": " + exception);
                System.out.println(executionCount);
                if (executionCount >= retries) return false;

                if (exception instanceof NoHttpResponseException) {
                    // Retry if the server dropped connection on us
                    return true;
                }

                if (exception instanceof java.net.SocketException) {
                    // Retry if the server dropped connection on us
                    return true;
                }

                if (exception instanceof org.apache.http.client.ClientProtocolException) {
                    return true;
                }

                Boolean b = (Boolean)
                        context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
                boolean sent = (b != null && b.booleanValue());
                if (!sent) {
                    // Retry if the request has not been sent fully or
                    // if it's OK to retry methods that have been sent
                    return true;
                }

                // otherwise do not retry
                return false;
            }
        };

        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 0);
        HttpConnectionParams.setSoTimeout(httpParams, 0);
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        client.setHttpRequestRetryHandler(myRetryHandler);
        client.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while(it.hasNext())

                {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
                return 30 * 1000;
            }
        });

        String content = null;
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);

            BasicResponseHandler responseHandler = new BasicResponseHandler();
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                content = responseHandler.handleResponse(response);
            }
            else {
                throw new UnexpectedHttpResponseCodeException(response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase());
            }
        }
        finally {
            client.getConnectionManager().shutdown();
        }
        return content;
    }
}
