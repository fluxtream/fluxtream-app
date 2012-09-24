package com.fluxtream.connectors.flickr;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractUpdater;
import com.fluxtream.connectors.updaters.RateLimitReachedException;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.fluxtream.utils.HttpUtils.fetch;
import static com.fluxtream.utils.Utils.hash;

/**
 * @author candide
 * 
 */
@Component
@Updater(prettyName = "Flickr", value = 11, objectTypes = FlickrPhotoFacet.class, extractor = FlickrFacetExtractor.class)
public class FlickrUpdater extends AbstractUpdater {

    @Autowired
	GuestService guestService;

	private static final int ITEMS_PER_PAGE = 20;
	private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	public FlickrUpdater() {
		super();
	}

	String sign(Map<String, String> parameters) throws NoSuchAlgorithmException {
		String toSign = env.get("flickrConsumerSecret");
		SortedSet<String> eachKey = new TreeSet<String>(parameters.keySet());
		for (String key : eachKey)
			toSign += key + parameters.get(key);
		String sig = hash(toSign);
		return sig;
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws RateLimitReachedException, Exception {
		// taking care of resetting the data if things went wrong before
		if (!connectorUpdateService.isHistoryUpdateCompleted(
				updateInfo.getGuestId(), connector().getName(), -1))
			apiDataService.eraseApiData(updateInfo.getGuestId(), connector(),
					-1);
		int retrievedItems = ITEMS_PER_PAGE;
		for (int page = 0; retrievedItems == ITEMS_PER_PAGE; page++) {
			JSONObject feed = retrievePhotoHistory(updateInfo, 0,
					System.currentTimeMillis(), page);
            if (feed.has("stat")) {
                String stat = feed.getString("stat");
                if (stat.equalsIgnoreCase("fail")) {
                    String message = feed.getString("message");
                    throw new RuntimeException("Could not retrieve Flickr history: " + message);
                }
            }
			JSONObject photosWrapper = feed.getJSONObject("photos");

			if (photosWrapper != null) {
				JSONArray photos = photosWrapper.getJSONArray("photo");
				retrievedItems = photos.size();
				apiDataService.cacheApiDataJSON(updateInfo, feed, -1, -1);
			} else
				break;
		}
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		int retrievedItems = ITEMS_PER_PAGE;
		ApiUpdate lastSuccessfulUpdate = connectorUpdateService
				.getLastSuccessfulUpdate(updateInfo.apiKey.getGuestId(),
						connector());
		for (int page = 0; retrievedItems == ITEMS_PER_PAGE; page++) {
			JSONObject feed = retrievePhotoHistory(updateInfo,
					lastSuccessfulUpdate.ts, System.currentTimeMillis(), page);
			JSONObject photosWrapper = feed.getJSONObject("photos");

			if (photosWrapper != null) {
				JSONArray photos = photosWrapper.getJSONArray("photo");
				retrievedItems = photos.size();
				apiDataService.cacheApiDataJSON(updateInfo, feed, -1, -1);
			} else
				break;
		}
	}

	private JSONObject retrievePhotoHistory(UpdateInfo updateInfo, long from,
			long to, int page) throws Exception {
		long then = System.currentTimeMillis();

		String api_key = env.get("flickrConsumerKey");
		String nsid = guestService.getApiKeyAttribute(
				updateInfo.apiKey.getGuestId(), connector(), "nsid");
		String token = guestService.getApiKeyAttribute(
				updateInfo.apiKey.getGuestId(), connector(), "token");

        // The start/end upload dates should be in the form of a unix timestamp (see http://www.flickr.com/services/api/flickr.people.getPhotos.htm)
		String startDate = String.valueOf(from / 1000);
		String endDate = String.valueOf(to / 1000);

		Map<String, String> params = new HashMap<String, String>();
		params.put("method", "flickr.people.getPhotos");
        params.put("per_page", String.valueOf(ITEMS_PER_PAGE));
        params.put("page", String.valueOf(page));
        params.put("api_key", api_key);
		params.put("user_id", nsid);
		params.put("auth_token", token);
		params.put("format", "json");
		params.put("nojsoncallback", "1");
		params.put("extras", "date_upload,date_taken,description,geo,tags");
		params.put("min_upload_date", startDate);
		params.put("max_upload_date", endDate);

		String api_sig = sign(params);

        String searchPhotosUrl = "http://api.flickr.com/services/rest/" +
                                 "?method=flickr.people.getPhotos&api_key=" + api_key +
                                 "&per_page=" + ITEMS_PER_PAGE +
                                 "&page=" + page +
                                 "&api_key=" + api_key +
                                 "&user_id=" + nsid +
                                 "&auth_token=" + token +
                                 "&format=json&nojsoncallback=1&extras=date_upload,date_taken,description,geo,tags" +
                                 "&min_upload_date=" + startDate +
                                 "&max_upload_date=" + endDate +
                                 "&api_sig=" + api_sig;
        searchPhotosUrl = searchPhotosUrl.replace(" ", "%20");
		String photosJson = null;
		try {
			photosJson = fetch(searchPhotosUrl, env);
			countSuccessfulApiCall(updateInfo.getGuestId(),
					updateInfo.objectTypes, then, searchPhotosUrl);
		} catch (Exception e) {
			countFailedApiCall(updateInfo.getGuestId(), updateInfo.objectTypes,
					then, searchPhotosUrl, Utils.stackTrace(e));
			throw e;
		}

		if (photosJson == null || photosJson.equals(""))
			throw new Exception(
					"empty json string returned from flickr API call");

		JSONObject feed = JSONObject.fromObject(photosJson);

		return feed;
	}
}
