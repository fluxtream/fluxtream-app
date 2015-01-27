package org.fluxtream.connectors.lastfm;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.fluxtream.core.utils.HttpUtils.fetch;

/**
 * @author candide
 *
 */

@Component
@Updater(prettyName = "Last FM", value = 10, objectTypes = {LastFmRecentTrackFacet.class}, extractor = LastFmFacetExtractor.class,
        deviceNickname = "Last_FM",
         bodytrackResponder = LastFmBodytrackResponder.class, defaultChannels = {"Last_FM.tracks"})
public class LastFmUpdater extends AbstractUpdater {

    private static final int ITEMS_PER_PAGE = 200;

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    public LastFmUpdater() {
        super();
    }

    @Override
    protected void updateConnectorDataHistory(UpdateInfo updateInfo) throws Exception {
        final ObjectType recent_track = ObjectType.getObjectType(connector(), "recent_track");
        if (updateInfo.objectTypes().contains(recent_track)) {
            retrieveTracks(updateInfo, 0, "recenttracks");
        }
    }

    @Override
    public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        if (updateInfo.objectTypes().contains(ObjectType.getObjectType(connector(), "recent_track"))) {
            retrieveRecentTracks(updateInfo);
        }
    }

    @Override
    public void setDefaultChannelStyles(ApiKey apiKey) {
        BodyTrackHelper.ChannelStyle channelStyle = new BodyTrackHelper.ChannelStyle();
        channelStyle.timespanStyles = new BodyTrackHelper.MainTimespanStyle();
        channelStyle.timespanStyles.defaultStyle = new BodyTrackHelper.TimespanStyle();
        channelStyle.timespanStyles.defaultStyle.fillColor = "#fd4938";
        channelStyle.timespanStyles.defaultStyle.borderColor = "#fd4938";
        channelStyle.timespanStyles.defaultStyle.borderWidth = 2;
        channelStyle.timespanStyles.defaultStyle.top = 0.0;
        channelStyle.timespanStyles.defaultStyle.bottom = 1.0;
        channelStyle.timespanStyles.values = new HashMap();

        BodyTrackHelper.TimespanStyle stylePart = new BodyTrackHelper.TimespanStyle();
        stylePart.top = 0.25;
        stylePart.bottom = 0.75;
        stylePart.fillColor = "#fd4938";
        stylePart.borderColor = "#fd4938";
        channelStyle.timespanStyles.values.put("on",stylePart);

        bodyTrackHelper.setBuiltinDefaultStyle(apiKey.getGuestId(), apiKey.getConnector().getName(), "tracks", channelStyle);
    }

    private void retrieveTracks(final UpdateInfo updateInfo, final long fromTime, final String tracksType) throws Exception {
        int page =  0;
        do {
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
                break;
            page++;
        } while  (true);
    }

    private void retrieveRecentTracks(UpdateInfo updateInfo) throws Exception {
        LastFmRecentTrackFacet lastRetrievedTrack = jpaDaoService.findOne("lastfm.recent_track.newest", LastFmRecentTrackFacet.class, updateInfo.apiKey.getId());
        long fromTime = 0;
        if (lastRetrievedTrack != null) {
            fromTime = lastRetrievedTrack.time;
        }
        retrieveTracks(updateInfo, fromTime, "recenttracks");
    }

    private JSONObject getTracks(UpdateInfo updateInfo, long from, long to, int page, String tracksType) throws Exception {
        String api_key = guestService.getApiKeyAttribute(updateInfo.apiKey, "lastfmConsumerKey");
        String username = guestService.getApiKeyAttribute(updateInfo.apiKey, "username");
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
