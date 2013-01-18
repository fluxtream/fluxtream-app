package com.fluxtream.connectors.lastfm;

import static com.fluxtream.utils.HttpUtils.fetch;

import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluxtream.connectors.ObjectType;
import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.services.JPADaoService;

/**
 * @author candide
 * 
 */

@Component
@Updater(prettyName = "Last FM", value = 10, objectTypes = {
		LastFmLovedTrackFacet.class, LastFmRecentTrackFacet.class }, extractor = LastFmFacetExtractor.class)
@JsonFacetCollection(LastFmFacetVOCollection.class)
public class LastFmUpdater extends AbstractUpdater {

	private static final int ITEMS_PER_PAGE = 200;

	@Autowired
	JPADaoService jpaDaoService;

	public LastFmUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws RateLimitReachedException, Exception {
		ObjectType recentTrackObjectType = ObjectType.getObjectType(
				connector(), "recent_track");
		ObjectType lovedTrackObjectType = ObjectType.getObjectType(connector(),
				"loved_track");
		if (updateInfo.objectTypes().contains(recentTrackObjectType)) {
			retrieveRecentTracksHistory(updateInfo, recentTrackObjectType);
		} else {
			retrieveLovedTracksHistory(updateInfo, lovedTrackObjectType);
		}
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		if (updateInfo.objectTypes().contains(
				ObjectType.getObjectType(connector(), "recent_track"))) {
			retrieveRecentTracks(updateInfo);
		} else {
			retrieveLovedTracks(updateInfo);
		}
	}

	private void retrieveLovedTracksHistory(UpdateInfo updateInfo,
			ObjectType lovedTrackObjectType) throws Exception {
		// taking care of resetting the data if things went wrong before
		if (!connectorUpdateService.isHistoryUpdateCompleted(
				updateInfo.apiKey,
				lovedTrackObjectType.value()))
			apiDataService.eraseApiData(updateInfo.apiKey,
					lovedTrackObjectType.value());
		int retrievedItems = ITEMS_PER_PAGE;
		for (int page = 0; retrievedItems >= ITEMS_PER_PAGE; page++) {
			JSONObject lovedTracksHistory = getLovedTracks(updateInfo, 0,
					System.currentTimeMillis(), page);
			JSONObject lovedTracks = lovedTracksHistory
					.getJSONObject("lovedtracks");
			if (lovedTracks.containsKey("track")
					&& lovedTracks.get("track") instanceof JSONArray) {
				JSONArray tracks = lovedTracks.getJSONArray("track");
				retrievedItems = tracks.size();
				apiDataService.cacheApiDataJSON(updateInfo, lovedTracksHistory,
						-1, -1);
			} else
				break;
		}
	}

	private void retrieveRecentTracksHistory(UpdateInfo updateInfo,
			ObjectType recentTrackObjectType) throws Exception {
		// taking care of resetting the data if things went wrong before
		if (!connectorUpdateService.isHistoryUpdateCompleted(
				updateInfo.apiKey, recentTrackObjectType.value()))
			apiDataService.eraseApiData(updateInfo.apiKey,
					recentTrackObjectType.value());
		int retrievedItems = ITEMS_PER_PAGE;
		for (int page = 0; retrievedItems >= ITEMS_PER_PAGE; page++) {
			JSONObject recentTracksHistory = getRecentTracks(updateInfo, 0,
					System.currentTimeMillis(), page);
			JSONObject recentTracks = recentTracksHistory
					.getJSONObject("recenttracks");
			if (recentTracks.containsKey("track")
					&& recentTracks.get("track") instanceof JSONArray) {
				JSONArray tracks = recentTracks.getJSONArray("track");
				retrievedItems = tracks.size();
				apiDataService.cacheApiDataJSON(updateInfo,
						recentTracksHistory, -1, -1);
			} else
				break;
		}
	}

	private void retrieveRecentTracks(UpdateInfo updateInfo) throws Exception {
		int retrievedItems = ITEMS_PER_PAGE;
		LastFmRecentTrackFacet lastRetrievedTrack = jpaDaoService.findOne("lastfm.recent_track.newest",
				LastFmRecentTrackFacet.class, updateInfo.getGuestId());
        long fromTime = 0;
        if(lastRetrievedTrack!=null) {
            fromTime=lastRetrievedTrack.time;
        }
		for (int page = 0; retrievedItems >= ITEMS_PER_PAGE; page++) {
			JSONObject mostRecentTracks = getRecentTracks(updateInfo,
					fromTime, System.currentTimeMillis(), page);
			JSONObject recentTracks = mostRecentTracks
					.getJSONObject("recenttracks");
			if (recentTracks.containsKey("track")
					&& recentTracks.get("track") instanceof JSONArray) {
				JSONArray tracks = recentTracks.getJSONArray("track");
				retrievedItems = tracks.size();
				apiDataService.cacheApiDataJSON(updateInfo, mostRecentTracks, -1,
						-1);
			} else
				break;
		}
	}

	private void retrieveLovedTracks(UpdateInfo updateInfo) throws Exception {
		int retrievedItems = ITEMS_PER_PAGE;
		LastFmLovedTrackFacet lastRetrievedTrack = jpaDaoService.findOne("lastfm.loved_track.newest",
				LastFmLovedTrackFacet.class, updateInfo.getGuestId());
        long fromTime = 0;
        if(lastRetrievedTrack!=null) {
            fromTime=lastRetrievedTrack.time;
        }
		for (int page = 0; retrievedItems >= ITEMS_PER_PAGE; page++) {
			JSONObject mostRecentTracks = getLovedTracks(updateInfo,
					fromTime, System.currentTimeMillis(), page);
			JSONObject lovedTracks = mostRecentTracks
					.getJSONObject("lovedtracks");
			if (lovedTracks.containsKey("track")
					&& lovedTracks.get("track") instanceof JSONArray) {
				JSONArray tracks = lovedTracks.getJSONArray("track");
				retrievedItems = tracks.size();
				apiDataService
						.cacheApiDataJSON(updateInfo, mostRecentTracks, -1, -1);
			} else
				break;
		}
	}

	private JSONObject getLovedTracks(UpdateInfo updateInfo, long from,
			long to, int page) throws Exception {
		String api_key = env.get("lastfmConsumerKey");
		String username = updateInfo.apiKey.getAttributeValue("username", env);
		long then = System.currentTimeMillis();

		String lovedTracksJson = null;
		String query = "http://ws.audioscrobbler.com/2.0/?method=user.getlovedtracks&user="
				+ username
				+ "&from="
				+ from
				/ 1000
				+ "&to="
				+ to
				/ 1000
				+ "&api_key="
				+ api_key
				+ "&limit="
				+ ITEMS_PER_PAGE
				+ "&format=json" + "&page=" + page;
		try {
			lovedTracksJson = fetch(query);
			countSuccessfulApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, query);
			JSONObject result = JSONObject.fromObject(lovedTracksJson);
			return result;
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes,
					then, query, Utils.stackTrace(e));
			throw e;
		}
	}

	private JSONObject getRecentTracks(UpdateInfo updateInfo, long from,
			long to, int page) throws Exception {
		String api_key = env.get("lastfmConsumerKey");
		String username = updateInfo.apiKey.getAttributeValue("username", env);
		long then = System.currentTimeMillis();

		String recentTracksJson = null;
		String query = "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user="
				+ username
				+ "&from="
				+ from
				/ 1000
				+ "&to="
				+ to
				/ 1000
				+ "&api_key="
				+ api_key
				+ "&limit="
				+ ITEMS_PER_PAGE
				+ "&format=json" + "&page=" + page;
		try {
			recentTracksJson = fetch(query);
			countSuccessfulApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, query);
			JSONObject result = JSONObject.fromObject(recentTracksJson);
			return result;
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes,
					then, query, Utils.stackTrace(e));
			throw e;
		}
	}

}
