package com.fluxtream.connectors.picasa;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fluxtream.connectors.annotations.JsonFacetCollection;
import com.fluxtream.connectors.annotations.Updater;
import com.fluxtream.connectors.updaters.AbstractGoogleOAuthUpdater;
import com.fluxtream.connectors.updaters.UpdateInfo;
import com.fluxtream.domain.ApiUpdate;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.Utils;
import com.google.gdata.client.Query;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.PhotoEntry;

@Component
@Updater(prettyName = "Picasa", value = 13, objectTypes = { PicasaPhotoFacet.class })
@JsonFacetCollection(PicasaFacetVOCollection.class)
public class PicasaUpdater extends AbstractGoogleOAuthUpdater {

	public GuestService guestService;

	public PicasaUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		if (!connectorUpdateService.isHistoryUpdateCompleted(
				updateInfo.getGuestId(), connector().getName(), updateInfo.objectTypes))
			apiDataService.eraseApiData(updateInfo.getGuestId(),
					connector());
		loadHistory(updateInfo, 0, System.currentTimeMillis());
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
		ApiUpdate lastUpdate = connectorUpdateService.getLastSuccessfulUpdate(
				updateInfo.apiKey.getGuestId(), connector());
		loadHistory(updateInfo, lastUpdate.ts, System.currentTimeMillis());
	}

	private void loadHistory(UpdateInfo updateInfo, long from, long to)
			throws Exception {
		String queryUrl = "request url not set yet";
		long then = System.currentTimeMillis();
		List<PhotoEntry> entries = null;
		try {
			URL feedUrl = new URL(
					"https://picasaweb.google.com/data/feed/api/user/default");

			PicasawebService myService = new PicasawebService("fluxtream");

			myService.setOAuthCredentials(
					getOAuthParameters(updateInfo.apiKey),
					new OAuthHmacSha1Signer());
			Query myQuery = new Query(feedUrl);
			myQuery.setStringCustomParameter("kind", "photo");
			myQuery.setStringCustomParameter("max-results", "1000000");

			AlbumFeed resultFeed = myService.query(myQuery, AlbumFeed.class);

			List<PhotoEntry> allEntries = resultFeed.getPhotoEntries();
			
			if (from!=0) {
				entries = new ArrayList<PhotoEntry>();
				for (PhotoEntry photoEntry : allEntries) {
					if (photoEntry.getTimestamp().getTime()>from)
						entries.add(photoEntry);
				}
			} else
				entries = allEntries;
			
		} catch (Exception e) {
			countFailedApiCall(updateInfo.apiKey.getGuestId(),
					updateInfo.objectTypes, then, queryUrl);
			throw new Exception("Could not get Picasa photos: " + e.getMessage() + "\n" + Utils.stackTrace(e));
		}

		countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
				updateInfo.objectTypes, then, queryUrl);

		if (entries != null) {
			for (PhotoEntry photoEntry : entries) {
				PicasaPhotoFacet sentry = new PicasaPhotoFacet();
				sentry.description = photoEntry.getTitle().getPlainText();
				sentry.photoId = photoEntry.getId();
				sentry.thumbnailUrl = photoEntry.getMediaThumbnails().get(0)
						.getUrl();
				List<MediaContent> mediaContents = photoEntry.getMediaContents();
				for (MediaContent mediaContent : mediaContents) {
					if (mediaContent.getMedium().equals("image")) {
						sentry.photoUrl = mediaContent.getUrl();
					}
				}
				sentry.start = photoEntry.getTimestamp().getTime();
				sentry.end = photoEntry.getTimestamp().getTime();
				apiDataService.cacheApiDataObject(
						updateInfo, -1, -1,
						sentry);
			}
		} else
			throw new Exception("Null entries when loading picasa history");
	}

}
