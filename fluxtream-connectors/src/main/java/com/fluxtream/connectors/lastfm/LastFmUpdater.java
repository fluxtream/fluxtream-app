package com.fluxtream.connectors.lastfm;

import java.io.IOException;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.services.JPADaoService;
import com.fluxtream.utils.UnexpectedHttpResponseCodeException;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.HttpUtils.fetch;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Last FM", value = 10, objectTypes = {LastFmRecentTrackFacet.class}, extractor = LastFmFacetExtractor.class)
@JsonFacetCollection(LastFmFacetVOCollection.class)
public class LastFmUpdater extends AbstractUpdater {

    private static final int ITEMS_PER_PAGE = 200;

    @Autowired
    JPADaoService jpaDaoService;

    public LastFmUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        final ObjectType recent_track = ObjectType.getObjectType(connector(), "recent_track");
        // restarting from scratch if an error occured
        if (updateInfo.objectTypes().contains(recent_track)) {
            apiDataService.eraseApiData(updateInfo.apiKey, recent_track.value());
            retrieveTracks(updateInfo, 0, "recenttracks", 0);
        }
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        if (updateInfo.objectTypes().contains(ObjectType.getObjectType(connector(), "recent_track"))) {
            retrieveRecentTracks(updateInfo);
        }
    }

    private void retrieveTracks(final UpdateInfo updateInfo, final long fromTime, final String tracksType, final int page) throws Exception {
        JSONObject tracks = getTracks(updateInfo, fromTime, System.currentTimeMillis(), page, tracksType);
        JSONObject tracksObject = tracks.getJSONObject(tracksType);
        if (tracksObject.containsKey("track") && (tracksObject.get("track") instanceof JSONArray
                || tracksObject.get("track") instanceof JSONObject))
            apiDataService.cacheApiDataJSON(updateInfo, tracks, -1, -1);
        else
            return;
        final JSONObject metadata = tracksObject.getJSONObject("@attr");
        final int currentPage = Integer.valueOf(metadata.getString("page"));
        final int totalPages = Integer.valueOf(metadata.getString("totalPages"));
        if (totalPages-1 == currentPage)
            return;
        retrieveTracks(updateInfo, fromTime, tracksType, currentPage+1);
    }

    private void retrieveRecentTracks(UpdateInfo updateInfo) throws Exception {
        LastFmRecentTrackFacet lastRetrievedTrack = jpaDaoService.findOne("lastfm.recent_track.newest", LastFmRecentTrackFacet.class, updateInfo.getGuestId());
        long fromTime = 0;
        if (lastRetrievedTrack != null) {
            fromTime = lastRetrievedTrack.time;
        }
        retrieveTracks(updateInfo, fromTime, "recenttracks", 0);
    }

    private JSONObject getTracks(UpdateInfo updateInfo, long from, long to, int page, String tracksType) throws Exception {
        String api_key = guestService.getApiKeyAttribute(updateInfo.apiKey, "lastfmConsumerKey");
        String username = updateInfo.apiKey.getAttributeValue("username", env);
        long then = System.currentTimeMillis();

        String query = String.format("http://ws.audioscrobbler.com/2.0/?method=user.get%s&user=%s&from=%s&to=%s&api_key=%s&limit=%s&format=%s&page=%s",
                                     tracksType, username, from/1000, to/1000, api_key, ITEMS_PER_PAGE, "json", page);
        try {
            String tracksJson = fetch(query);
            countSuccessfulApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query);
            JSONObject result = JSONObject.fromObject(tracksJson);
            return result;
        }
        catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query, Utils.stackTrace(e),
                               e.getHttpResponseCode(), e.getHttpResponseMessage());
            throw e;
        } catch (IOException e) {
            reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, query, Utils.stackTrace(e), "I/O");
            throw e;
        }
    }
}
