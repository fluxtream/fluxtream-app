package glacier.picasa;

import org.fluxtream.connectors.annotations.Updater;
import org.fluxtream.connectors.updaters.AbstractUpdater;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.services.GuestService;
import org.springframework.stereotype.Component;

@Component
@Updater(prettyName = "Picasa", value = 13, objectTypes = { PicasaPhotoFacet.class })
public class PicasaUpdater extends AbstractUpdater {

	public GuestService guestService;

	public PicasaUpdater() {
		super();
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		//if (!connectorUpdateService.isHistoryUpdateCompleted(
		//		updateInfo.getGuestId(), connector().getName(),
		//		updateInfo.objectTypes))
		//	apiDataService.eraseApiData(updateInfo.getGuestId(), connector());
		//loadHistory(updateInfo, 0, System.currentTimeMillis());
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        //ApiUpdate lastUpdate = connectorUpdateService.getLastSuccessfulUpdate(
			//	updateInfo.apiKey.getGuestId(), connector());
        //final long from = (lastUpdate == null) ? 0 : lastUpdate.ts;
        //loadHistory(updateInfo, from, System.currentTimeMillis());
	}

	private void loadHistory(UpdateInfo updateInfo, long from, long to)
			throws Exception {

        // As of January 2013 Picasa updating no longer works, so just skip out
        // at this point to avoid gratuitous errors on accounts which have
        // picasa connectors
        return;
     //
	//	String queryUrl = "request url not set yet";
	//	long then = System.currentTimeMillis();
	//	List<PhotoEntry> entries = null;
	//	try {
	//		URL feedUrl = new URL(
	//				"https://picasaweb.google.com/data/feed/api/user/default");
    //
	//		PicasawebService myService = new PicasawebService("");
    //
	//		myService.setOAuthCredentials(
	//				getOAuthParameters(updateInfo.apiKey),
	//				new OAuthHmacSha1Signer());
	//		Query myQuery = new Query(feedUrl);
	//		myQuery.setStringCustomParameter("kind", "photo");
	//		myQuery.setStringCustomParameter("max-results", "1000000");
    //
     //       // record the request url
     //       queryUrl = myQuery.getUrl().toString();
    //
	//		AlbumFeed resultFeed = myService.query(myQuery, AlbumFeed.class);
    //
	//		List<PhotoEntry> allEntries = resultFeed.getPhotoEntries();
    //
	//		if (from != 0) {
	//			entries = new ArrayList<PhotoEntry>();
	//			for (PhotoEntry photoEntry : allEntries) {
	//				if (photoEntry.getUpdated().getValue() > from)
	//					entries.add(photoEntry);
	//			}
	//		} else
	//			entries = allEntries;
    //
	//	} catch (Exception e) {
	//		countFailedApiCall(updateInfo.apiKey.getGuestId(),
	//				updateInfo.objectTypes, then, queryUrl, Utils.stackTrace(e));
	//		throw new Exception("Could not get Picasa photos: "
	//				+ e.getMessage() + "\n" + Utils.stackTrace(e));
	//	}
    //
	//	countSuccessfulApiCall(updateInfo.apiKey.getGuestId(),
	//			updateInfo.objectTypes, then, queryUrl);
    //
	//	if (entries != null) {
	//		for (PhotoEntry photoEntry : entries) {
	//			PicasaPhotoFacet sentry = new PicasaPhotoFacet();
	//			sentry.comment = photoEntry.getDescription().getPlainText();
	//			sentry.photoId = photoEntry.getId();
	//			List<MediaThumbnail> mediaThumbnails = photoEntry
	//					.getMediaThumbnails();
	//			sentry.thumbnailUrl = mediaThumbnails.get(0).getUrl();
	//			JSONArray thumbnailsArray = new JSONArray();
	//			for (MediaThumbnail mediaThumbnail : mediaThumbnails) {
	//				JSONObject jsonThumbnail = new JSONObject();
	//				int height = mediaThumbnail.getHeight();
	//				int width = mediaThumbnail.getWidth();
	//				String url = mediaThumbnail.getUrl();
	//				jsonThumbnail.accumulate("height", height)
	//						.accumulate("width", width).accumulate("url", url);
	//				thumbnailsArray.add(jsonThumbnail);
	//			}
	//			sentry.thumbnailsJson = thumbnailsArray.toString();
	//			List<MediaContent> mediaContents = photoEntry
	//					.getMediaContents();
	//			for (MediaContent mediaContent : mediaContents) {
	//				if (mediaContent.getMedium().equals("image")) {
	//					sentry.photoUrl = mediaContent.getUrl();
	//				}
	//			}
	//			sentry.start = photoEntry.getTimestamp().getTime();
	//			sentry.end = photoEntry.getTimestamp().getTime();
	//			apiDataService.cacheApiDataObject(updateInfo, -1, -1, sentry);
	//		}
	//	} else
	//		throw new Exception("Null entries when loading picasa history");
	}

}
