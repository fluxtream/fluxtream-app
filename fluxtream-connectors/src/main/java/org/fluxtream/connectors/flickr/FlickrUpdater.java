package org.fluxtream.connectors.flickr;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.annotations.Updater;
import org.fluxtream.core.connectors.location.LocationFacet;
import org.fluxtream.core.connectors.updaters.AbstractUpdater;
import org.fluxtream.core.connectors.updaters.AuthExpiredException;
import org.fluxtream.core.connectors.updaters.UpdateFailedException;
import org.fluxtream.core.connectors.updaters.UpdateInfo;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.ChannelMapping;
import org.fluxtream.core.domain.Tag;
import org.fluxtream.core.services.ApiDataService;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.JPADaoService;
import org.fluxtream.core.services.MetadataService;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.UnexpectedHttpResponseCodeException;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.fluxtream.core.utils.HttpUtils.fetch;
import static org.fluxtream.core.utils.Utils.hash;

/**
 * @author candide
 * 
 */
@Component
@Updater(prettyName = "Flickr",
         value = 11,
         objectTypes = FlickrPhotoFacet.class,
         defaultChannels = {"Flickr.photo"})
public class FlickrUpdater extends AbstractUpdater {

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
	GuestService guestService;

    private static final DateTimeFormatter format = DateTimeFormat
            .forPattern("yyyy-MM-dd HH:mm:ss").withZone(DateTimeZone.UTC);

	private static final int ITEMS_PER_PAGE = 500;

    @Autowired
    JPADaoService jpaDaoService;

    @Autowired
    MetadataService metadataService;

	public FlickrUpdater() {
		super();
	}

	String sign(ApiKey apiKey, Map<String, String> parameters) throws NoSuchAlgorithmException {
		String toSign = guestService.getApiKeyAttribute(apiKey, "flickrConsumerSecret");
		SortedSet<String> eachKey = new TreeSet<String>(parameters.keySet());
		for (String key : eachKey)
			toSign += key + parameters.get(key);
		String sig = hash(toSign);
		return sig;
	}

	@Override
	protected void updateConnectorDataHistory(UpdateInfo updateInfo)
			throws Exception {
		// taking care of resetting the data if things went wrong before
		//if (!connectorUpdateService.isHistoryUpdateCompleted( updateInfo.apiKey, -1))
		//	apiDataService.eraseApiData(updateInfo.apiKey, -1);
        int page = 0, pages;
		do {
            page++;
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
                pages = photosWrapper.getInt("pages");
                page = photosWrapper.getInt("page");
				JSONArray photos = photosWrapper.getJSONArray("photo");
                createOrUpdatePhotos(photos, updateInfo);
            } else
				break;
		} while (page<pages);
	}

	@Override
	public void updateConnectorData(UpdateInfo updateInfo) throws Exception {
        Long lastUpdatedTime = getLastUpdatedTime(updateInfo);
        // taking care of resetting the data if we are coming from a database that
        // hasn't tracked the updatedate yet
        if (lastUpdatedTime==null||lastUpdatedTime==0) {
            apiDataService.eraseApiData(updateInfo.apiKey, -1);
            updateConnectorDataHistory(updateInfo);
            return;
        }
        int page = 0, pages;
        do {
            page++;
            JSONObject feed = retrieveRecentlyUpdatedPhotos(updateInfo, lastUpdatedTime, page);
                if (feed.has("stat")) {
                String stat = feed.getString("stat");
                if (stat.equalsIgnoreCase("fail")) {
                    String message = feed.getString("message");
                    if (message.indexOf("Invalid auth token")!=-1) {
                        throw new AuthExpiredException();
                    } else
                        throw new UpdateFailedException("Could not retrieve flickr recently updated photos: " + message,
                                                        true,
                                                        ApiKey.PermanentFailReason.unknownReason(message));
                }
            }
            JSONObject photosWrapper = feed.getJSONObject("photos");
            pages = photosWrapper.getInt("pages");
            page = photosWrapper.getInt("page");

            if (photosWrapper != null) {
                JSONArray photos = photosWrapper.getJSONArray("photo");
                createOrUpdatePhotos(photos, updateInfo);
            } else
                break;
        } while (page<pages);
	}
    
    private void createOrUpdatePhotos(final JSONArray photos, final UpdateInfo updateInfo) throws Exception {
        final List<LocationFacet> locationResources = new ArrayList<LocationFacet>();
        for (int i=0; i<photos.size(); i++) {
            final JSONObject photo = photos.getJSONObject(i);
            String flickrId = photo.getString("id");
            final ApiDataService.FacetQuery facetQuery = new ApiDataService.FacetQuery("e.apiKeyId=? AND e.flickrId=?",
                                                                                       updateInfo.apiKey.getId(), flickrId);
            final ApiDataService.FacetModifier<FlickrPhotoFacet> facetModifier = new ApiDataService.FacetModifier<FlickrPhotoFacet>() {
                @Override
                public FlickrPhotoFacet createOrModify(FlickrPhotoFacet origFacet, final Long apiKeyId) {
                    FlickrPhotoFacet facet=origFacet;

                    try {
                    if (facet==null) {
                        facet = new FlickrPhotoFacet(updateInfo.apiKey.getId());
                        facet.flickrId = photo.getString("id");
                        facet.api = updateInfo.apiKey.getConnector().value();
                        facet.guestId = updateInfo.apiKey.getGuestId();
                        facet.timeUpdated = System.currentTimeMillis();
                    }
                    facet.owner = photo.getString("owner");
                    facet.secret = photo.getString("secret");
                    facet.server = photo.getString("server");
                    facet.farm = photo.getString("farm");
                    facet.title = photo.getString("title");
                    final JSONObject descriptionObject = photo.getJSONObject("description");
                    if (descriptionObject != null) {
                        facet.comment = descriptionObject.getString("_content");
                    }
                    facet.ispublic = Integer.valueOf(photo.getString("ispublic")) == 1;
                    facet.isfriend = Integer.valueOf(photo.getString("isfriend")) == 1;
                    facet.isfamily = Integer.valueOf(photo.getString("isfamily")) == 1;
                    final String datetaken = photo.getString("datetaken");
                    final DateTime dateTime = format.parseDateTime(datetaken);
                    facet.startTimeStorage = facet.endTimeStorage = toTimeStorage(dateTime.getYear(), dateTime.getMonthOfYear(),
                                                                                  dateTime.getDayOfMonth(), dateTime.getHourOfDay(),
                                                                                  dateTime.getMinuteOfHour(), 0);
                    facet.date = (new StringBuilder(String.valueOf(dateTime.getYear())).append("-")
                                          .append(pad(dateTime.getMonthOfYear())).append("-")
                                          .append(pad(dateTime.getDayOfMonth()))).toString();
                    facet.datetaken = dateTime.getMillis();
                    facet.start = dateTime.getMillis();
                    facet.end = dateTime.getMillis();
                    facet.dateupload = photo.getLong("dateupload")*1000;
                    if (photo.has("lastupdate"))
                        facet.dateupdated = photo.getLong("lastupdate")*1000;
                    facet.accuracy = photo.getInt("accuracy");
                    facet.addTags(photo.getString("tags"), Tag.SPACE_DELIMITER);
                    if (photo.getString("latitude")!=null && photo.getString("longitude")!=null) {
                        final Float latitude = Float.valueOf(photo.getString("latitude"));
                        final Float longitude = Float.valueOf(photo.getString("longitude"));
                        if (latitude!=0 && longitude!=0) {
                            facet.latitude = latitude;
                            facet.longitude = longitude;
                            addLocation(updateInfo, locationResources, facet, dateTime);
                        }
                    }
                    return facet;
                    }
                       catch (Throwable e) {
                           // Attempt to parse this photo failed.  Return the original facet.
                           // If it was null then nothing is persisted.  If it was not null then
                           // whatever changes we made before we died will be persisted, which is
                           // really the best we can do

                           // TODO: generate notification of failed import using getPhotoUrl
                           return(origFacet);
                       }
                }
            };
            // we could use the resulting value (facet) from this call if we needed to do further processing on it (e.g. passing it on to the datastore)
            apiDataService.createOrReadModifyWrite(FlickrPhotoFacet.class, facetQuery, facetModifier, updateInfo.apiKey.getId());
        }

        if (locationResources.size()>0)
            metadataService.updateLocationMetadata(updateInfo.getGuestId(), locationResources);
    }

   public String getPhotoUrl(final JSONObject photoJson) {
      // TODO: Return a string of the form http://www.flickr.com/photos/<owner>/<id>/edit-details/
      //  photo.getString("id");
      //  photo.getString("owner");
      return null;
   }


    private Long getLastUpdatedTime(final UpdateInfo updateInfo) {
        final String entityName = JPAUtils.getEntityName(FlickrPhotoFacet.class);
        final List<FlickrPhotoFacet> facets = jpaDaoService.executeQueryWithLimit("SELECT facet from " + entityName + " facet WHERE facet.apiKeyId=? ORDER BY facet.dateupdated DESC", 1, FlickrPhotoFacet.class, updateInfo.apiKey.getId());
        if (facets.size()==0)
            return new Long(0);
        final Long dateupdated = facets.get(0).dateupdated;
        if (dateupdated!=null) {
            return dateupdated + 1000;
        } else return null;
    }

    private JSONObject retrievePhotoHistory(UpdateInfo updateInfo, long from,
			long to, int page) throws Exception {

        List<ChannelMapping> mappings = bodyTrackHelper.getChannelMappings(updateInfo.apiKey, ObjectType.getObjectType(updateInfo.apiKey.getConnector(),"photo").value());
        if (mappings.size() == 0){
            ChannelMapping mapping = new ChannelMapping();
            mapping.deviceName = "Flickr";
            mapping.channelName = "photo";
            mapping.timeType = ChannelMapping.TimeType.local;
            mapping.channelType = ChannelMapping.ChannelType.photo;
            mapping.guestId = updateInfo.getGuestId();
            mapping.apiKeyId = updateInfo.apiKey.getId();
            mapping.objectTypeId = ObjectType.getObjectType(updateInfo.apiKey.getConnector(),"photo").value();
            bodyTrackHelper.persistChannelMapping(mapping);
        }

		long then = System.currentTimeMillis();

        // The start/end upload dates should be in the form of a unix timestamp (see http://www.flickr.com/services/api/flickr.people.getPhotos.htm)
		String startDate = String.valueOf(from / 1000);
		String endDate = String.valueOf(to / 1000);

        final Map<String, String> otherParams = new HashMap<String, String>();
        otherParams.put("method", "flickr.people.getPhotos");
        otherParams.put("min_upload_date", startDate);
        otherParams.put("max_upload_date", endDate);

        String searchPhotosUrl = buildFlickrAPIUrl(updateInfo, page, otherParams);

		String photosJson;
		try {
			photosJson = fetch(searchPhotosUrl);
			countSuccessfulApiCall(updateInfo.apiKey,
					updateInfo.objectTypes, then, searchPhotosUrl);
        } catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes,
                               then, searchPhotosUrl, Utils.stackTrace(e),
                               e.getHttpResponseCode(), e.getHttpResponseMessage());
            if (e.getHttpResponseCode()>=400 && e.getHttpResponseCode()<500)
                throw new UpdateFailedException("Unexpected response code: " + e.getHttpResponseCode(), new Exception(), true,
                                                ApiKey.PermanentFailReason. clientError(e.getHttpResponseCode(), e.getHttpResponseMessage()));
            throw new UpdateFailedException(e, false, null);
		} catch (IOException e) {
			reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes, then, searchPhotosUrl,
                                Utils.stackTrace(e), "I/O");
			throw e;
		}

		if (photosJson == null || photosJson.equals(""))
			throw new Exception(
					"empty json string returned from flickr API call");

		JSONObject feed = JSONObject.fromObject(photosJson);

		return feed;
	}

    private JSONObject retrieveRecentlyUpdatedPhotos(UpdateInfo updateInfo, long lastUpdate, int page) throws Exception {
        long then = System.currentTimeMillis();

        // The start/end upload dates should be in the form of a unix timestamp (see http://www.flickr.com/services/api/flickr.people.getPhotos.htm)
        String lastupdate= String.valueOf(lastUpdate / 1000);

        final Map<String, String> otherParams = new HashMap<String, String>();
        otherParams.put("method", "flickr.photos.recentlyUpdated");
        otherParams.put("min_date", lastupdate);

        String searchPhotosUrl = buildFlickrAPIUrl(updateInfo, page, otherParams);

        String photosJson;
        try {
            photosJson = fetch(searchPhotosUrl);
            countSuccessfulApiCall(updateInfo.apiKey,
                                   updateInfo.objectTypes, then, searchPhotosUrl);
        } catch (UnexpectedHttpResponseCodeException e) {
            countFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes,
                               then, searchPhotosUrl, Utils.stackTrace(e),
                               e.getHttpResponseCode(), e.getHttpResponseMessage());
            if (e.getHttpResponseCode()>=400 && e.getHttpResponseCode()<500)
                throw new UpdateFailedException("Unexpected response code: " + e.getHttpResponseCode(), new Exception(), true,
                                                ApiKey.PermanentFailReason.clientError(e.getHttpResponseCode(), e.getHttpResponseMessage()));
            throw new UpdateFailedException(e, false, null);
        } catch (IOException e) {
            reportFailedApiCall(updateInfo.apiKey, updateInfo.objectTypes,
                               then, searchPhotosUrl, Utils.stackTrace(e), "I/O");
            throw e;
        }

        if (photosJson == null || photosJson.equals(""))
            throw new Exception(
                    "empty json string returned from flickr API call");

        JSONObject feed = JSONObject.fromObject(photosJson);

        return feed;
    }

    private String buildFlickrAPIUrl(final UpdateInfo updateInfo, final int page, final Map<String, String> otherParams) throws NoSuchAlgorithmException {
        final String api_key = guestService.getApiKeyAttribute(updateInfo.apiKey, "flickrConsumerKey");
        final String nsid = guestService.getApiKeyAttribute(
                updateInfo.apiKey, "nsid");
        final String token = guestService.getApiKeyAttribute(updateInfo.apiKey, "token");

        final Map<String, String> params = new HashMap<String, String>();
        params.put("api_key", api_key);
        params.put("user_id", nsid);
        params.put("auth_token", token);
        params.put("per_page", String.valueOf(ITEMS_PER_PAGE));
        params.put("page", String.valueOf(page));
        params.put("format", "json");
        params.put("nojsoncallback", "1");
        params.put("extras", "date_upload,date_taken,description,geo,tags,last_update");
        params.putAll(otherParams);

        final String api_sig = sign(updateInfo.apiKey, params);

        final StringBuilder urlBuilder = new StringBuilder("https://api.flickr.com/services/rest/?");

        for (Map.Entry<String, String> parameter : params.entrySet())
            urlBuilder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("&");
        urlBuilder.append("api_sig=").append(api_sig);
        String searchPhotosUrl = urlBuilder.toString();
        searchPhotosUrl = searchPhotosUrl.replace(" ", "%20");
        return searchPhotosUrl;
    }

    private static String pad(int i) {
        return i<10
               ? (new StringBuilder("0").append(i)).toString()
               : String.valueOf(i);
    }

    private String toTimeStorage(int year, int month, int day, int hours,
                                   int minutes, int seconds) {
        //yyyy-MM-dd'T'HH:mm:ss.SSS
        return (new StringBuilder()).append(year)
                .append("-").append(pad(month)).append("-")
                .append(pad(day)).append("T").append(pad(hours))
                .append(":").append(pad(minutes)).append(":")
                .append(pad(seconds)).append(".000").toString();
    }

    private void addLocation(final UpdateInfo updateInfo, final List<LocationFacet> locationResources,
                             final FlickrPhotoFacet facet, final DateTime dateTime) {
        LocationFacet locationResource = new LocationFacet();
        locationResource.guestId = facet.guestId;
        locationResource.latitude = facet.latitude;
        locationResource.longitude = facet.longitude;
        locationResource.source = LocationFacet.Source.FLICKR;
        locationResource.timestampMs = dateTime.getMillis();
        locationResource.apiKeyId = updateInfo.apiKey.getId();
        locationResource.start = dateTime.getMillis();
        locationResource.end = dateTime.getMillis();
        locationResource.api = updateInfo.apiKey.getConnector().value();

        locationResources.add(locationResource);
    }

}
