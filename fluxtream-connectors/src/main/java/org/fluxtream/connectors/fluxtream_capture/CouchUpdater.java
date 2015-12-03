package org.fluxtream.connectors.fluxtream_capture;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
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
import org.fluxtream.core.Configuration;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.services.*;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fluxtream.core.api.CouchDBController.maybeHash;
import static org.fluxtream.core.utils.Utils.sanitize;

/**
 * Created by candide on 11/02/15.
 */
@Component
@Controller
@Transactional(readOnly=true)
public class CouchUpdater {

    private final String NEW_TOPIC = "newTopic";
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

    @Autowired
    Configuration env;

    @Autowired
    SettingsService settingsService;

    @PersistenceContext
    EntityManager em;

    public enum CouchDatabaseName {
        TOPICS, OBSERVATIONS
    }

    final String lollipopStyle = "{\"styles\":[{\"type\":\"line\",\"show\":false,\"lineWidth\":1}," +
            "{\"radius\":0,\"fill\":false,\"type\":\"lollipop\",\"show\":true,\"lineWidth\":1}," +
            "{\"radius\":2,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}," +
            "{\"marginWidth\":5,\"verticalOffset\":7," +
            "\"numberFormat\":\"###,##0\",\"type\":\"value\",\"show\":true}]," +
            "\"comments\":" +
            "{\"styles\":[{\"radius\":3,\"fill\":true,\"type\":\"point\",\"show\":true,\"lineWidth\":1}]," +
            "\"verticalMargin\":4,\"show\":true}}";

    @Transactional(readOnly=false)
    public void updateCaptureData(UpdateInfo updateInfo, FluxtreamCaptureUpdater updater, CouchDatabaseName couchDatabaseName) throws Exception {
        if (guestService.getApiKeyAttribute(updateInfo.apiKey, "couchDB.userToken")==null) return;
        String rootURL = getRootCouchDbURL(updateInfo, couchDatabaseName);
        long lastSeq = 0;
        try {
            lastSeq = Long.valueOf(guestService.getApiKeyAttribute(updateInfo.apiKey, couchDatabaseName.name() + "_last_seq"));
        } catch (Exception e) {}

        // Fetch and load changes, starting with lastSeq, fetching at most maxToFetch each pass
        final int maxToFetch = 100;

        while (true) {
            String URL = rootURL + "/_changes?since=" + lastSeq + "&limit=" + maxToFetch + "&include_docs=true";
            long newLastSeq;
            JSONArray changes;

            try {
                String base64URLSafeUsername = getCouchDBLegalUsername(updateInfo);
                String couchdbPassword = guestService.getApiKeyAttribute(updateInfo.apiKey, "couchDB.userToken");
                byte[] encodedCredentials = getBase64EncodedCredentials(base64URLSafeUsername, couchdbPassword);
                JSONObject json = JSONObject.fromObject(fetchRetrying(URL, encodedCredentials, 3));
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
                JSONObject doc = change.getJSONObject("doc");
                if (change.optBoolean("deleted", false)) {
                    if (couchDatabaseName==CouchDatabaseName.TOPICS) {
                        String topicId = doc.getString("_id");
                        TypedQuery<ChannelMapping> query = em.createQuery(String.format("SELECT cm FROM ChannelMapping cm WHERE cm.internalChannelName='topic_%s'", topicId), ChannelMapping.class);
                        List<ChannelMapping> channelMappings = query.getResultList();
                        if (channelMappings.size()>0) {
                            ChannelMapping channelMapping = channelMappings.get(0);
                            removeCalendarChannel(updateInfo, channelMapping);
                            maybeRemoveChannelMapping(topicId, channelMapping);
                        }
                    }
                } else {
                    switch (couchDatabaseName) {
                        case OBSERVATIONS:
                            FluxtreamObservationFacet observationFacet = createOrUpdateObservation(updateInfo, rootURL, doc);
                            if (observationFacet != null) {
                                apiDataService.persistFacet(observationFacet);
                                newFacets.add(observationFacet);
                            }
                            break;
                        case TOPICS:
                            updateInfo.setContext(NEW_TOPIC, false);
                            FluxtreamTopicFacet topicFacet = createOrUpdateTopic(updateInfo, rootURL, doc);
                            Boolean newTopic = (Boolean) updateInfo.getContext(NEW_TOPIC);
                            if (newTopic)
                                addCalendarChannel(updateInfo, doc.getString("name"));
                            if (topicFacet != null) {
                                apiDataService.persistFacet(topicFacet);
                                newFacets.add(topicFacet);
                            }
                            break;
                    }
                }
            }

            if (couchDatabaseName==CouchDatabaseName.OBSERVATIONS)
                // Write the new set of observations into the datastore
                bodyTrackStorageService.storeApiData(updateInfo.apiKey, newFacets);
            else
                storeChannelMappings(newFacets, updateInfo.apiKey.getId(), updateInfo.getGuestId());

            lastSeq = newLastSeq;

            // Write lastSeq back to apiKeyAttributes
            guestService.setApiKeyAttribute(updateInfo.apiKey, couchDatabaseName.name() + "_last_seq", String.valueOf(lastSeq));
        }

    }

    /**
     * Remove Channel Mapping if there are no associated observations (i.e. the user created a topic, then deleted them and didn't
     * make any observations for that topic)
     * @param topicId
     * @param channelMapping
     */
    @Transactional(readOnly=false)
    private void maybeRemoveChannelMapping(String topicId, ChannelMapping channelMapping) {
        TypedQuery<FluxtreamObservationFacet> query = em.createQuery("SELECT observation FROM Facet_FluxtreamCaptureObservation observation WHERE observation.topicId=?1", FluxtreamObservationFacet.class);
        query.setParameter(1, topicId);
        List<FluxtreamObservationFacet> resultList = query.getResultList();
        if (resultList.size()==0) {
            em.refresh(channelMapping);
            em.remove(channelMapping);
        }
    }

    @Transactional(readOnly=false)
    private void addCalendarChannel(UpdateInfo updateInfo, String name) {
        String channelIdentifier = updateInfo.apiKey.getConnector().getDeviceNickname() + "." + name;
        String[] channelsForConnector = settingsService.getChannelsForConnector(updateInfo.getGuestId(), updateInfo.apiKey.getConnector());
        List<String> newChannels = new ArrayList<String>();
        boolean alreadyAdded = false;
        for (String channelName : channelsForConnector) {
            if (channelName.equals(channelIdentifier)) {
                alreadyAdded = true;
                break;
            }
            newChannels.add(channelName);
        }
        if (!alreadyAdded) {
            newChannels.add(channelIdentifier);
            settingsService.setChannelsForConnector(updateInfo.getGuestId(), updateInfo.apiKey.getConnector(), newChannels.toArray(new String[newChannels.size()]));
        }
    }

    @Transactional(readOnly=false)
    private void removeCalendarChannel(UpdateInfo updateInfo, ChannelMapping channelMapping) {
        String channelIdentifier = updateInfo.apiKey.getConnector().getDeviceNickname() + "." + channelMapping.getChannelName();
        String[] channelsForConnector = settingsService.getChannelsForConnector(updateInfo.getGuestId(), updateInfo.apiKey.getConnector());
        List<String> newChannels = new ArrayList<String>();
        boolean channelWasFound = false;
        for (String channelName : channelsForConnector) {
            if (channelName.equals(channelIdentifier)) {
                channelWasFound = true;
                continue;
            }
            newChannels.add(channelName);
        }
        if (channelWasFound)
            settingsService.setChannelsForConnector(updateInfo.getGuestId(), updateInfo.apiKey.getConnector(), newChannels.toArray(new String[newChannels.size()]));
    }

    @Transactional(readOnly=false)
    private void storeChannelMappings(List<AbstractFacet> newFacets, long apiKeyId, long guestId) {
        for (AbstractFacet newFacet : newFacets) {
            FluxtreamTopicFacet topic = (FluxtreamTopicFacet) newFacet;
            Query query = em.createQuery("SELECT mapping FROM ChannelMapping mapping WHERE mapping.deviceName='FluxtreamCapture' AND mapping.internalChannelName=? AND mapping.guestId=?");
            query.setParameter(1, "topic_" + topic.fluxtreamId);
            query.setParameter(2, guestId);
            List<ChannelMapping> mappings = query.getResultList();
            if (mappings.size()>0) {
                ChannelMapping mapping = mappings.get(0);
                String previousChannelName = mapping.getChannelName();
                if (!mapping.getChannelName().equals(topic.name)){
                    String noClashChannelName = createNoClashChannelName(sanitize(topic.name), guestId);
                    mapping.setChannelName(noClashChannelName);
                }
                query = em.createQuery("SELECT style FROM ChannelStyle style WHERE style.deviceName='FluxtreamCapture' AND style.channelName=? AND style.guestId=?");
                query.setParameter(1, previousChannelName);
                query.setParameter(2, guestId);
                List<ChannelStyle> styles = query.getResultList();
                if (styles.size()>0)
                    styles.get(0).channelName = topic.name;
            } else {
                // add increment to avoid name clashes
                String noClashChannelName = createNoClashChannelName(sanitize(topic.name), guestId);
                ChannelMapping mapping = new ChannelMapping(apiKeyId, guestId,
                        ChannelMapping.ChannelType.data, ChannelMapping.TimeType.gmt,
                        2, "FluxtreamCapture", noClashChannelName,
                        "FluxtreamCapture", "topic_" + topic.fluxtreamId);
                mapping.setCreationType(ChannelMapping.CreationType.dynamic);
                bodytrackHelper.setBuiltinDefaultStyle(guestId, "FluxtreamCapture", topic.name, lollipopStyle);
                em.persist(mapping);
            }
        }
    }

    private String createNoClashChannelName(String sanitizedTopicName, long guestId) {
        TypedQuery<String> channelNameQuery = em.createQuery("SELECT mapping.channelName FROM ChannelMapping mapping WHERE mapping.deviceName='FluxtreamCapture' AND mapping.guestId=?1 AND mapping.channelName LIKE ?2", String.class);
        channelNameQuery.setParameter(1, guestId);
        channelNameQuery.setParameter(2, sanitizedTopicName+"%");
        List<String> channelNames = channelNameQuery.getResultList();
        int maxIndex = -1;
        if (channelNames.contains(sanitizedTopicName))
            maxIndex = 1;
        Pattern incrementPattern = Pattern.compile("\\[(\\d*)\\]");
        for (String channelName : channelNames) {
            Matcher m = incrementPattern.matcher(channelName);
            while (m.find()) {
                String s = m.group(1);
                try {
                    int increment = Integer.valueOf(s);
                    if (increment>maxIndex) maxIndex = increment;
                } catch (Throwable t) {}
            }
        }
        String result = sanitizedTopicName;
        if (maxIndex!=-1)
            result += "_[" + (maxIndex+1) + "]";
        return result;
    }

    @RequestMapping(value="/fluxtream_capture/mappings/resync", method = RequestMethod.POST)
    public void resyncChannelMappings(HttpServletResponse response) throws IOException {
        ApiKey apiKey = guestService.getApiKey(AuthHelper.getGuestId(), Connector.getConnector("fluxtream_capture"));
        mapTopicsToChannels(apiKey);
        response.getWriter().write("resynced");
    }

    private void mapTopicsToChannels(ApiKey apiKey) {
        Query query = em.createQuery("SELECT topic FROM Facet_FluxtreamCaptureTopic topic WHERE topic.apiKeyId=?");
        query.setParameter(1, apiKey.getId());
        List<AbstractFacet> allTopics = query.getResultList();
        storeChannelMappings(allTopics, apiKey.getId(), apiKey.getGuestId());
    }

    @RequestMapping(value="/fluxtream_capture/user/mappings/resync", method = RequestMethod.POST)
    @Secured("ROLE_ADMIN")
    public void resyncUserChannelMappings(HttpServletResponse response,
                                          @RequestParam("username") String username) throws IOException {
        Guest guest = guestService.getGuest(username);
        ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector("fluxtream_capture"));
        mapTopicsToChannels(apiKey);
        response.getWriter().write("resynced");
    }

    private byte[] getBase64EncodedCredentials(String base64URLSafeUsername, String couchdbPassword) {
        String userPassword = base64URLSafeUsername + ":" + couchdbPassword;
        return Base64.encodeBase64(userPassword.getBytes());
    }

    private String getCouchDBLegalUsername(UpdateInfo updateInfo) {
        String base64URLSafeUsername = null;
        Guest guest = guestService.getGuestById(updateInfo.getGuestId());
        base64URLSafeUsername = maybeHash(guest.username);
        return base64URLSafeUsername;
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
                                        facet.guestId = updateInfo.apiKey.getGuestId();
                                        facet.api = updateInfo.apiKey.getConnector().value();
                                    }

                                    facet.topicId = observation.getString("topicId");

                                    facet.timeUpdatedOnDevice = ISODateTimeFormat.dateTime().withZoneUTC().parseDateTime(observation.getString("updateTime")).getMillis();

                                    facet.timeUpdated = System.currentTimeMillis();

                                    facet.timeZone = observation.getString("timezone");

                                    final DateTime happened = ISODateTimeFormat.dateTimeNoMillis()
                                            .parseDateTime(observation.getString("observationTime"));
                                    facet.start = facet.end = happened.getMillis();

                                    facet.comment = observation.optString("comment", null);

                                    if (observation.has("value")&&observation.get("value")!=null&&!observation.getString("value").equals("null"))
                                        facet.value = observation.getInt("value");

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

    private FluxtreamTopicFacet createOrUpdateTopic(final UpdateInfo updateInfo, final String rootURL, final JSONObject topic) {
        try {
            // fluxtreamId is unique for each topic
            final String fluxtreamId = topic.getString("_id");

            FluxtreamTopicFacet ret =
                    apiDataService.createOrReadModifyWrite(FluxtreamTopicFacet.class,
                            new ApiDataService.FacetQuery(
                                    "e.apiKeyId = ? AND e.fluxtreamId = ?",
                                    updateInfo.apiKey.getId(),
                                    fluxtreamId),
                            new ApiDataService.FacetModifier<FluxtreamTopicFacet>() {
                                // Throw exception if it turns out we can't make sense of the topic's JSON
                                // This will abort the transaction
                                @Override
                                public FluxtreamTopicFacet createOrModify(FluxtreamTopicFacet facet, Long apiKeyId) {
                                    if (facet == null) {
                                        facet = new FluxtreamTopicFacet(updateInfo.apiKey.getId());
                                        facet.fluxtreamId = fluxtreamId;
                                        // auto-populate the facet's tags field with the name of the topic (e.g. "Food", "Back Pain", etc.)
                                        facet.guestId = updateInfo.apiKey.getGuestId();
                                        facet.api = updateInfo.apiKey.getConnector().value();
                                        updateInfo.setContext(NEW_TOPIC, true);
                                    }

                                    facet.topicNumber = topic.getInt("topicNumber");
                                    facet.timeUpdated = System.currentTimeMillis();
                                    facet.name = topic.getString("name").trim();

                                    return facet;
                                }
                            }, updateInfo.apiKey.getId());
            return ret;

        } catch (Throwable e) {
            // Couldn't makes sense of topic's JSON
            return null;
        }
    }

    // Returns root URL for fluxtream capture database, without trailing / (e.g. http://hostname/databasename)
    private String getRootCouchDbURL(final UpdateInfo updateInfo, CouchDatabaseName couchDatabaseName) {
        final String couchdbHost = env.get("couchdb.host");
        final String couchdbPort = env.get("couchdb.port");
        String base64URLSafeUsername = getCouchDBLegalUsername(updateInfo);
        switch (couchDatabaseName) {
            case OBSERVATIONS:
                return String.format("http://%s:%s/self_report_db_observations_%s", couchdbHost, couchdbPort, base64URLSafeUsername);
            default:
                return String.format("http://%s:%s/self_report_db_topics_%s", couchdbHost, couchdbPort, base64URLSafeUsername);
        }
    }

    String fetchRetrying(final String url, byte[] encodedCredentials, final int retries) throws IOException, UnexpectedHttpResponseCodeException {
        HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
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
            get.addHeader("Authorization", "Basic " + new String(encodedCredentials));
            get.addHeader("Content-Type", "application/json;charset=utf-8");
            get.addHeader("Accept", "application/json;charset=utf-8");
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
