package com.fluxtream.api;

import java.awt.Dimension;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.TimeUnit;
import com.fluxtream.connectors.vos.AbstractPhotoFacetVO;
import com.fluxtream.domain.Guest;
import com.fluxtream.mvc.controllers.ControllerHelper;
import com.fluxtream.mvc.models.StatusModel;
import com.fluxtream.services.BodyTrackStorageService;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.PhotoService;
import com.fluxtream.services.impl.BodyTrackHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.newrelic.api.agent.NewRelic.setTransactionName;

@Path("/bodytrack")
@Component("RESTBodytrackController")
@Scope("request")
public class BodyTrackController {

    private static final Logger LOG = Logger.getLogger(BodyTrackController.class);

    @Autowired
	GuestService guestService;
	
	@Autowired
	BodyTrackStorageService bodytrackStorageService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    PhotoService photoService;

	Gson gson = new Gson();

	@Autowired
	Configuration env;
	
	@POST
	@Path("/uploadHistory")
	@Produces({ MediaType.APPLICATION_JSON })
	public String loadHistory(@QueryParam("username") String username,
			@QueryParam("connectorName") String connectorName) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        setTransactionName(null, "POST /bodytrack/uploadHistory");
        StatusModel status;
        try{
            Guest guest = guestService.getGuest(username);

            if (!checkForPermissionAccess(guest.getId())){
                status = new StatusModel(false, "Failure!");
            }
            else{
                bodytrackStorageService.storeInitialHistory(guest.getId(), connectorName);
                status = new StatusModel(true, "Success!");
            }
        }
        catch (Exception e){
            status = new StatusModel(false,"Failure!");
        }
        return gson.toJson(status);
    }

    @DELETE
    @Path("/users/{UID}/views/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteBodytrackView(@PathParam("UID") Long uid, @PathParam("id") long viewId){
        setTransactionName(null, "DELETE /bodytrack/users/{UID}/views/{id}");
        StatusModel status;
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            bodyTrackHelper.deleteView(uid, viewId);
            status = new StatusModel(true,"successfully deleted view " + viewId);
        }
        catch (Exception e){
            status = new StatusModel(false,"failed to delete view " + viewId);
        }
        return gson.toJson(status);
    }

    @POST
    @Path("/upload")
    @Consumes({MediaType.MULTIPART_FORM_DATA,MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON})
    public String uploadToBodytrack(@FormParam("dev_nickname") String deviceNickanme, @FormParam("channel_names") String channels,
                                    @FormParam("data") String data){
        setTransactionName(null, "POST /bodytrack/upload");
        StatusModel status;
        try{
            long uid = ControllerHelper.getGuestId();
            Type channelsType =  new TypeToken<Collection<String>>(){}.getType();
            Type dataType = new TypeToken<List<List<Long>>>(){}.getType();
            bodyTrackHelper.uploadToBodyTrack(uid, deviceNickanme, (Collection<String>)gson.fromJson(channels, channelsType), (List<List<Object>>)gson.fromJson(data, dataType));
            status = new StatusModel(true,"Upload successful!");
        }
        catch (Exception e){
            status = new StatusModel(false,"Upload failed!");
        }
        return gson.toJson(status);
    }

    @GET
    @Path("/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
    @Produces({MediaType.APPLICATION_JSON})
    public String fetchTile(@PathParam("UID") Long uid, @PathParam("DeviceNickname") String deviceNickname,
                                   @PathParam("ChannelName") String channelName, @PathParam("Level") int level, @PathParam("Offset") long offset){
        setTransactionName(null, "GET /bodytrack/tiles/{UID}/" + deviceNickname + "." + channelName + "/{Level}.{Offset}.json");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.fetchTile(uid, deviceNickname, channelName, level, offset);
        } catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/views")
    @Produces({MediaType.APPLICATION_JSON})
    public String getViews(@PathParam("UID") Long uid) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/views");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.listViews(uid);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/views/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public String bodyTrackView(@PathParam("UID") Long uid, @PathParam("id") long id) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/views/{id}");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            String result = bodyTrackHelper.getView(uid,id);
            return result == null ? gson.toJson(new StatusModel(false,"Failed to get view")) : result;
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @POST
    @Path("/users/{UID}/views")
    @Produces({MediaType.APPLICATION_JSON})
    public String setView(@PathParam("UID") Long uid, @FormParam("name") String name, @FormParam("data") String data) {
        setTransactionName(null, "POST /bodytrack/users/{UID}/views");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.saveView(uid, name, data);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/sources/list")
    @Produces({MediaType.APPLICATION_JSON})
    public String getSourceList(@PathParam("UID") Long uid) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/sources/list");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.listSources(uid);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path(value = "/users/{UID}/sources/{source}/default_graph_specs")
    @Produces({MediaType.APPLICATION_JSON})
    public String bodyTrackGetDefaultGraphSpecs(@PathParam("UID") Long uid, @PathParam("source") String name) {
        setTransactionName(null, "GET /bodytrack/users/{UID}/sources/{source}/default_graph_specs");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            return bodyTrackHelper.getSourceInfo(uid, name);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @POST
    @Path("/users/{UID}/channels/{DeviceNickname}.{ChannelName}/set")
    @Produces({MediaType.APPLICATION_JSON})
    public String setDefaultStyle(@PathParam("UID") Long uid, @PathParam("DeviceNickname") String deviceNickname,
                                @PathParam("ChannelName") String channelName, @FormParam("user_default_style") String style) {
        setTransactionName(null, "POST /users/{UID}/channels/" + deviceNickname + "." + channelName + "/set");
        try{
            if (!checkForPermissionAccess(uid)){
                uid = null;
            }
            bodyTrackHelper.setDefaultStyle(uid,deviceNickname,channelName,style);
            return gson.toJson(new StatusModel(true, "Channel style set"));
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path("/photos/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
    @Produces({MediaType.APPLICATION_JSON})
    public String fetchPhotoTile(@PathParam("UID") Long uid,
                                 @PathParam("DeviceNickname") String deviceNickname,
                                 @PathParam("ChannelName") String channelName,
                                 @PathParam("Level") int level,
                                 @PathParam("Offset") long offset) {
        setTransactionName(null, "GET /bodytrack/photos/{UID}/" + deviceNickname + "." + channelName + "/{Level}.{Offset}.json");
        try {
            if (!checkForPermissionAccess(uid)) {
                uid = null;
            }

            if (uid == null) {
                return gson.toJson(new StatusModel(false, "Invalid User ID (null)"));
            }

            // first use Level and Offset to calculate the desired start and end times
            final long startTimeMillis = (long)(LevelOffsetHelper.offsetAtLevelToUnixTime(level, offset) * 1000);
            final long endTimeMillis = (long)(LevelOffsetHelper.offsetAtLevelToUnixTime(level, offset + 1) * 1000);

            // TODO: Not sure if this is correct for time zones...
            final TimeInterval timeInterval = new TimeInterval(startTimeMillis, endTimeMillis, TimeUnit.DAY, TimeZone.getTimeZone("UTC"));

            // fetch the photos for this time interval, and for the desired device/channel
            final SortedSet<PhotoService.Photo> photos  = photoService.getPhotos(uid, timeInterval, deviceNickname, channelName);

            // Define the min interval to be 1/20th of the span of the tile.  Value is in seconds
            final double minInterval = LevelOffsetHelper.levelToDuration(level) / 20.0;

            // Now filter the photos using the minInterval as follows:
            //  * min_interval specifies the minimum number of seconds between images.
            //  * Always include the first photo, set count to 1
            //  * When processing a given photo B, compare the time of this photo with the previous included
            //    photo A.  If image B is < min_interval seconds after image A, then increase count field in image
            //    A and ignore image B.  If image B is >= min_interval seconds after image A, then include image
            //    B with count=1
            PhotoItem photoA = null;
            final List<PhotoItem> filteredPhotos = new ArrayList<PhotoItem>();
            for (final PhotoService.Photo photoB : photos) {
                if (photoA == null) {
                    photoA = new PhotoItem(photoB);
                    filteredPhotos.add(photoA);
                }
                else {
                    // Already have a photoA, compare times to see if we should keep this one
                    final long photoBStartTimeSecs = photoB.getAbstractPhotoFacetVO().start / 1000;
                    if (photoBStartTimeSecs > (photoA.begin_d + minInterval)) {
                        // Enough of a gap between A and B, so keep this one and set to be new A
                        photoA = new PhotoItem(photoB);
                        filteredPhotos.add(photoA);
                    } else {
                        // Not enough of a gap, increment count on photoA
                        photoA.incrementCount();
                    }
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("BodyTrackController.fetchPhotoTile(): num photos filtered from " + photos.size() + " to " + filteredPhotos.size());
            }

            return gson.toJson(filteredPhotos);
        }
        catch (Exception e) {
            LOG.error("BodyTrackController.fetchPhotoTile(): Exception while trying to fetch photos: ", e);
            return gson.toJson(new StatusModel(false, "Access Denied"));
        }
    }

    @GET
    @Path("/photos/{UID}/{DeviceNickname}.{ChannelName}/{unixTime}/{count}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPhotosBeforeOrAfterTime(@PathParam("UID") long uid,
                                             @PathParam("DeviceNickname") String deviceNickname,
                                             @PathParam("ChannelName") String channelName,
                                             @PathParam("unixTime") long unixTimeInSecs,
                                             @PathParam("count") int desiredCount,
                                             @QueryParam("isBefore") boolean isGetPhotosBeforeTime,
                                             @QueryParam("tags") List<String> tags,
                                             @QueryParam("isMatchAllTags") boolean isMatchAllTags
                                             ) {
        setTransactionName(null, "GET /bodytrack/photos/{UID}/" + deviceNickname + "." + channelName + "/{unixTime}/{count}");

        try {
            if (!checkForPermissionAccess(uid)) {
                return gson.toJson(new StatusModel(false, "Invalid User ID (null)"));
             }

            final SortedSet<PhotoService.Photo> photos = photoService.getPhotos(uid,
                                                                                unixTimeInSecs * 1000,
                                                                                deviceNickname,
                                                                                channelName,
                                                                                desiredCount,
                                                                                isGetPhotosBeforeTime);

            // create the JSON response
            final List<PhotoItem> photoItems = new ArrayList<PhotoItem>();
            for (final PhotoService.Photo photo : photos) {
                photoItems.add(new PhotoItem(photo));
            }
            return gson.toJson(photoItems);
        }
        catch (Exception e) {
            LOG.error("BodyTrackController.getPhotosBeforeOrAfterTime(): Exception while trying to fetch log items: ", e);
            return gson.toJson(new StatusModel(false, "Access Denied"));
        }
    }

    private boolean checkForPermissionAccess(long targetUid){
        Guest guest = ControllerHelper.getGuest();
        return targetUid == guest.getId() || guest.hasRole(Guest.ROLE_ADMIN) || guest.hasRole(Guest.ROLE_ADMIN);
    }

    private static class PhotoItem {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

        boolean nsfw = false;
        long id;
        String description;
        String comment;
        long begin_d;
        String begin;
        long end_d;
        String end;
        String dev_id;
        String dev_nickname;
        String channel_name;
        String url;
        ArrayList<String> tags = new ArrayList<String>();
        ArrayList<PhotoItemThumbnail> thumbnails = new ArrayList<PhotoItemThumbnail>();
        int count = 1;

        public PhotoItem(final PhotoService.Photo photo) {
            final AbstractPhotoFacetVO photoFacetVO = photo.getAbstractPhotoFacetVO();


            this.id = photoFacetVO.id;
            this.description = photoFacetVO.description == null ? "" : photoFacetVO.description;
            this.comment = photoFacetVO.comment == null ? "" : photoFacetVO.comment;
            this.begin_d = photoFacetVO.start / 1000; // convert millis to seconds
            this.begin = DATE_TIME_FORMATTER.print(photoFacetVO.start);
            this.end_d = this.begin_d;
            this.end = this.begin;
            this.dev_id = photo.getConnector().getName();
            this.dev_nickname = photo.getConnector().prettyName();
            this.channel_name = photo.getObjectType().getName();
            final List<Dimension> thumbnailSizes = photoFacetVO.getThumbnailSizes();
            if ((thumbnailSizes != null) && (!thumbnailSizes.isEmpty())) {
                int i = 0;
                for (final Dimension thumbnailDimension : thumbnailSizes) {
                    final String url = photoFacetVO.getThumbnail(i);
                    thumbnails.add(new PhotoItemThumbnail(url, thumbnailDimension.width, thumbnailDimension.height));
                    i++;
                }
            }

            this.url = photoFacetVO.getPhotoUrl();
        }

        public void incrementCount() {
            this.count++;
        }
    }

    private static class PhotoItemThumbnail {
        String url;
        int width;
        int height;

        private PhotoItemThumbnail(final String url, final int width, final int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    private static final class LevelOffsetHelper {

        /** Number of bins per tile */
        private static final int NUM_BINS_PER_TILE = 512;

        /**
         * Returns the time in seconds of a bin in a tile at a given level. This is 2^level seconds, so level 0 bins
         * are 1 second, level 4 are 16 secs, etc.
         */
        private static double levelToBinSeconds(final int level) {
            return Math.pow(2, level);
        }

        /** Returns the duration in seconds of a tile at a given level.  For level 0 this is TILE_BIN_NUM seconds. */
        private static double levelToDuration(final int level) {
            return levelToBinSeconds(level) * NUM_BINS_PER_TILE;
        }

        /**
         * Returns the unixtime of the start of a tile at a given offset and level.  This is the duration at that
         * level times the offset.
         */
        private static double offsetAtLevelToUnixTime(final int level, final long offset) {
            return levelToDuration(level) * offset;
        }
    }
}
