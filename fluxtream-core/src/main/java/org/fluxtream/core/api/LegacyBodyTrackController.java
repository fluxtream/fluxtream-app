package org.fluxtream.core.api;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.BodyPartEntity;
import com.sun.jersey.multipart.MultiPart;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.commons.io.IOUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.SimpleTimeInterval;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.TimeUnit;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.connectors.bodytrackResponders.AbstractBodytrackResponder;
import org.fluxtream.core.connectors.fluxtream_capture.FluxtreamCapturePhoto;
import org.fluxtream.core.connectors.fluxtream_capture.FluxtreamCapturePhotoStore;
import org.fluxtream.core.connectors.vos.AbstractPhotoFacetVO;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.images.ImageOrientation;
import org.fluxtream.core.mvc.models.DimensionModel;
import org.fluxtream.core.mvc.models.StatusModel;
import org.fluxtream.core.mvc.models.TimespanModel;
import org.fluxtream.core.services.*;
import org.fluxtream.core.services.impl.BodyTrackHelper;
import org.fluxtream.core.utils.ConnectorUtils;
import org.fluxtream.core.utils.HashUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/bodytrack")
@Component("RESTLegacyBodytrackController")
@Api(value = "/bodytrack", description = "CSV export and import, timeline-related operations")
@Scope("request")
@Deprecated
public class LegacyBodyTrackController {

    private static final FlxLogger LOG = FlxLogger.getLogger(LegacyBodyTrackController.class);
    private static final FlxLogger LOG_DEBUG = FlxLogger.getLogger("Fluxtream");
    private static final int ONE_WEEK_IN_SECONDS = 604800;

    @Autowired
	GuestService guestService;

    @Autowired
    DataUpdateService dataUpdateService;

	@Autowired
	BodyTrackStorageService bodytrackStorageService;

    @Autowired
    BodyTrackHelper bodyTrackHelper;

    @Autowired
    BuddiesService buddiesService;

    @Autowired
    private FluxtreamCapturePhotoStore fluxtreamCapturePhotoStore;

    @Autowired
    PhotoService photoService;

	Gson gson = new Gson();

	@Autowired
	Configuration env;

    @Autowired
	protected ApiDataService apiDataService;

    @Autowired LegacyJsonResponseHelper jsonResponseHelper;

    @Autowired
    BeanFactory beanFactory;

    Pattern alphanumeric_dots_and_underscore = Pattern.compile("[0-9a-zA-Z_\\.]+");

    @GET
    @Path("/exportCSV/{UID}/fluxtream-export-from-{start}-to-{end}.csv")
    @ApiOperation(value = "CSV export <startTime -> endTime", response = String.class)
    @Deprecated
    public void exportCSV(@ApiParam(value="Channels", required=true) @QueryParam("channels") String channels,
                          @ApiParam(value="Start time (epoch seconds)", required=true) @PathParam("start") Long start,
                          @ApiParam(value="End time (epoch seconds)", required=true) @PathParam("end") Long end,
                          @ApiParam(value="User ID (must be ID of loggedIn user)", required=true) @PathParam("UID") Long uid,
                          @Context HttpServletResponse response){
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            if (!accessAllowed && trustedBuddy ==null) {
                uid = null;
            }

            if (uid == null) {
                throw new Exception();
            }

            response.setContentType("text/csv");

            String[] channelArray = gson.fromJson(channels,String[].class);

            bodyTrackHelper.exportToCSV(uid,Arrays.asList(channelArray),start,end,response.getOutputStream());
            response.flushBuffer();

        }
        catch (Exception e){
            try{
                response.sendError(500);
            } catch(Exception e2){
                System.err.print("failed to send error response");
                e2.printStackTrace();
            }
        }
    }

    @GET
    @Path("/exportCSV/{UID}/fluxtream-export-from-{start}.csv")
    @ApiOperation(value = "CSV export <startTime", response = String.class)
    @Deprecated
    public void exportCSVStartOnly(@ApiParam(value="Channels", required=true) @QueryParam("channels") String channels,
                                   @ApiParam(value="Start time (epoch seconds)", required=true) @PathParam("start") Long start,
                                   @ApiParam(value="User ID (must be ID of loggedIn user)", required=true) @PathParam("UID") Long uid,
                                   @Context HttpServletResponse response){
        exportCSV(channels,start,null,uid,response);
    }

    @GET
    @Path("/exportCSV/{UID}/fluxtream-export-to-{end}.csv")
    @ApiOperation(value = "CSV export <startTime", response = String.class)
    @Deprecated
    public void exportCSVEndOnly(@ApiParam(value="Channels", required=true) @QueryParam("channels") String channels,
                                 @ApiParam(value="End time (epoch seconds)", required=true) @PathParam("end") Long end,
                                 @ApiParam(value="User ID (must be ID of loggedIn user)", required=true) @PathParam("UID") Long uid,
                                 @Context HttpServletResponse response){
        exportCSV(channels,null,end,uid,response);
    }

    @GET
    @Path("/exportCSV/{UID}/fluxtream-export.csv")
    @ApiOperation(value = "CSV export all data for given user id", response = String.class)
    @Deprecated
    public void exportCSVNoParams(@ApiParam(value="Channels", required=true) @QueryParam("channels") String channels,
                                  @ApiParam(value="User ID (must be ID of loggedIn user)", required=true) @PathParam("UID") Long uid,
                                  @Context HttpServletResponse response){
        exportCSV(channels,null,null,uid,response);
    }


    @POST
	@Path("/uploadHistory")
    @Secured("ROLE_ADMIN")
	@Produces({ MediaType.APPLICATION_JSON })
    @Deprecated
	public String loadHistory(@QueryParam("username") String username,
			@QueryParam("connectorName") String connectorName) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
        StatusModel status;
        try{
            Guest guest = guestService.getGuest(username);

            if (!checkForPermissionAccess(guest.getId())){
                status = new StatusModel(false, "Failure!");
            }
            else {
                final ApiKey apiKey = guestService.getApiKey(guest.getId(), Connector.getConnector(connectorName));
                bodytrackStorageService.storeInitialHistory(apiKey);
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
    @ApiOperation(value = "Delete a view", response = String.class)
    @Produces({ MediaType.APPLICATION_JSON })
    @Deprecated
    public String deleteBodytrackView(@ApiParam(value="User ID (must be ID of loggedIn user)", required=true) @PathParam("UID") Long uid,
                                      @ApiParam(value="View ID", required=true) @PathParam("id") long viewId){
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
    @ApiOperation(value = "Upload binary data via multipart encoding for the current logged in user", response = String.class)
    @Deprecated
    public String uploadToBodytrack(@ApiParam(value="The device to upload the data for", required=true) @FormParam("dev_nickname") String deviceNickname,
                                    @ApiParam(value="JSON encoded array of channels being uploaded for", required=true) @FormParam("channel_names") String channels,
                                    @ApiParam(value="Multipart form data to be uploaded", required=true)  @FormParam("data") String data){
        StatusModel status;
        final List<String> channelNames = gson.fromJson(channels, new TypeToken<Collection<String>>(){}.getType());
        String illegalName = checkDeviceAndChannelNamesAreAlphanumericAndUnderscore(deviceNickname, channelNames);
        if (illegalName!=null) {
            status = new StatusModel(false, String.format("Illegal device nickname or channel name: %s; only alphanumeric and underscore are allowed", illegalName));
            return gson.toJson(status);
        }

        try{
            long guestId = AuthHelper.getGuestId();
            List<List<Object>> parsedData = new ArrayList<List<Object>>();

            //Gson doesn't seem to be able to handle arrays with mixed types nicely
            //This will parse through the array, we don't need much error checking because we want this to fail if the data is malformed
            JsonElement element = new JsonParser().parse(data);

            for (JsonElement e : element.getAsJsonArray()){
                List<Object> currentList = new ArrayList<Object>();
                parsedData.add(currentList);
                for (JsonElement dataPoint : e.getAsJsonArray()){
                    if (dataPoint instanceof JsonNull){
                        currentList.add(null);
                    }
                    else{
                        JsonPrimitive primitive = dataPoint.getAsJsonPrimitive();
                        if (primitive.isBoolean()){
                            currentList.add(primitive.getAsBoolean());
                        }
                        else if (primitive.isString()){
                            currentList.add(primitive.getAsString());
                        }
                        else{
                            currentList.add(primitive.getAsDouble());
                        }
                    }
                }
            }

            ApiKey fluxtreamCaptureApiKey = ensureFluxtreamCaptureApiKey(guestId);
            final BodyTrackHelper.BodyTrackUploadResult uploadResult = bodyTrackHelper.uploadToBodyTrack(fluxtreamCaptureApiKey, deviceNickname, channelNames, parsedData);
            if (uploadResult instanceof BodyTrackHelper.ParsedBodyTrackUploadResult){
                BodyTrackHelper.ParsedBodyTrackUploadResult parsedResult = (BodyTrackHelper.ParsedBodyTrackUploadResult) uploadResult;
                bodytrackStorageService.ensureDataChannelMappingsExist(fluxtreamCaptureApiKey, channelNames, deviceNickname);
                dataUpdateService.logBodyTrackDataUpdate(guestId,fluxtreamCaptureApiKey.getId(),null,parsedResult);
            } else {
                // what else can it be, really ?
                LOG.warn("Unexpected upload result type : " + uploadResult.getClass().getName() );
            }
            status = createStatusModelFromBodyTrackUploadResult(uploadResult);
        }
        catch (Exception e){
            status = new StatusModel(false,"Upload failed!");
        }
        return gson.toJson(status);
    }

    String checkDeviceAndChannelNamesAreAlphanumericAndUnderscore(String deviceNickname, List<String> channelNames) {
        Matcher matcher = alphanumeric_dots_and_underscore.matcher(deviceNickname);
        if (!matcher.matches()) return deviceNickname;
        for (String channelName : channelNames) {
            if (!alphanumeric_dots_and_underscore.matcher(channelName).matches())
                return channelName;
        }
        return null;
    }

    private ApiKey ensureFluxtreamCaptureApiKey(long guestId) {
        final Connector fluxtreamCapture = Connector.getConnector("fluxtream_capture");
        ApiKey apiKey = guestService.getApiKey(guestId, fluxtreamCapture);
        if (apiKey==null)
            apiKey = guestService.createApiKey(guestId, fluxtreamCapture);
        return apiKey;
    }

    @POST
    @Path("/jupload")
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Upload JSON data for current logged in user", response = String.class)
    @Deprecated
    public String uploadJsonToBodytrack(@ApiParam(value="The device to upload the data for", required=true) @QueryParam("dev_nickname")  String deviceNickname,
                                        @ApiParam(value="The data to upload", required=true) String body){
        StatusModel status;
        try{
            long uid = AuthHelper.getGuestId();
            status = createStatusModelFromBodyTrackUploadResult(bodyTrackHelper.uploadJsonToBodyTrack(uid, deviceNickname, body));
        }
        catch (Exception e){
            status = new StatusModel(false,"Upload failed!");
        }
        return gson.toJson(status);
    }

    private StatusModel createStatusModelFromBodyTrackUploadResult(final BodyTrackHelper.BodyTrackUploadResult uploadResult) {

        // check the uploadResult for success, and create a new StatusModel accordingly
        final StatusModel status;
        if (uploadResult.isSuccess()) {
            status = new StatusModel(true, "Upload successful!");
        }
        else {
            status = new StatusModel(false, "Upload failed!");
        }

        // Now try to parse the response in the uploadResult as JSON, inflating it into a BodyTrackUploadResponse
        BodyTrackUploadResponse bodyTrackUploadResponse = null;
        try {
            bodyTrackUploadResponse = gson.fromJson(uploadResult.getResponse(), BodyTrackUploadResponse.class);
        }
        catch (JsonSyntaxException e) {
            LOG.error("JsonSyntaxException while trying to convert the BodyTrackUploadResult response into a BodyTrackUploadResponse.  Response was [" + uploadResult.getResponse() + "]", e);
        }

        // add the response to the payload if non-null
        if (bodyTrackUploadResponse != null) {
            status.payload = bodyTrackUploadResponse;
        }
        return status;
    }

    // Based on code from http://aruld.info/handling-multiparts-in-restful-applications-using-jersey/ and http://stackoverflow.com/a/4687942
    @POST
    @Path("/photoUpload")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(value = "Upload a photo for the current logged in user", response = String.class)
    @Deprecated
    public Response handlePhotoUpload(@ApiParam(value="Connector to upload the photo for", required=true) @QueryParam("connector_name") final String connectorName,
                                      @ApiParam(value="Multipart encoded photo data", required=true) final MultiPart multiPart) {
        Response response;

        final Connector connector = Connector.getConnector(connectorName);
        if (connector != null) {
            // We currently only support photo uploads for the Fluxtream Capture connector
            if ("fluxtream_capture".equals(connector.getName())) {
                try {
                    if (multiPart != null) {
                        byte[] photoBytes = null;
                        String jsonMetadata = null;

                        // iterate over the body parts and pick out the "photo" and "metadata" parts
                        for (final BodyPart bodyPart : multiPart.getBodyParts()) {

                            final ContentDisposition contentDisposition = bodyPart.getContentDisposition();
                            if (contentDisposition != null) {
                                final Map<String, String> parameters = contentDisposition.getParameters();
                                if (parameters != null) {
                                    final String name = parameters.get("name");
                                    if ("photo".equals(name)) {
                                        // found the photo part
                                        final BodyPartEntity bodyPartEntity = (BodyPartEntity)bodyPart.getEntity();
                                        photoBytes = IOUtils.toByteArray(bodyPartEntity.getInputStream());
                                    }
                                    else if ("metadata".equals(name)) {
                                        // found the metadata part
                                        jsonMetadata = bodyPart.getEntityAs(String.class);
                                    }
                                }
                            }
                        }

                        // if we found both "photo" and "metadata"
                        if (photoBytes != null && jsonMetadata != null) {
                            final long guestId = AuthHelper.getGuestId();

                            // Record the upload request time.  In reality we don't really care about the upload
                            // time, but rather we want to ensure that the Fluxtream Capture connector is
                            // auto-added to the user's set of connectors, and this is the way to do it.  Note that the
                            // the field recorded here is merely the time of the last upload request *request* and says
                            // nothing about whether that request was actually successful
                            final ApiKey apiKey;
                            List<ApiKey> apiKeys = guestService.getApiKeys(guestId, connector);
                            if (apiKeys != null && !apiKeys.isEmpty()) {
                                apiKey = apiKeys.get(0);
                            }
                            else {
                                apiKey = guestService.createApiKey(guestId, connector);
                            }
                            guestService.setApiKeyAttribute(apiKey, "last_upload_request_time", String.valueOf(System.currentTimeMillis()));

                            // We have a photo and the metadata, so pass control to the FluxtreamCapturePhotoStore to save the photo
                            LOG_DEBUG.debug("BodyTrackController.savePhoto(" + guestId + ", " + photoBytes.length + ", " + jsonMetadata + ")");
                            try {
                                final FluxtreamCapturePhotoStore.OperationResult<FluxtreamCapturePhoto> result = fluxtreamCapturePhotoStore.saveOrUpdatePhoto(guestId, photoBytes, jsonMetadata, apiKey.getId());
                                final String photoStoreKey = result.getData().getPhotoStoreKey();
                                final Long databaseRecordId = result.getDatabaseRecordId();
                                LOG.info("BodyTrackController.handlePhotoUpload(): photo [" + photoStoreKey + "] " + result.getOperation() + " sucessfully!");
                                response = jsonResponseHelper.ok("photo " + result.getOperation() + " sucessfully!", new PhotoUploadResponsePayload(result.getOperation(), databaseRecordId, photoStoreKey));
                            }
                            catch (FluxtreamCapturePhotoStore.UnsupportedImageFormatException e) {
                                final String message = "UnsupportedImageFormatException while trying to save the photo";
                                LOG.error("BodyTrackController.handlePhotoUpload(): " + message);
                                response = jsonResponseHelper.unsupportedMediaType(message);
                            }
                            catch (FluxtreamCapturePhotoStore.InvalidDataException e) {
                                final String message = "InvalidDataException while trying to save the photo";
                                LOG.error("BodyTrackController.handlePhotoUpload(): " + message, e);
                                response = jsonResponseHelper.badRequest(message);
                            }
                            catch (FluxtreamCapturePhotoStore.StorageException e) {
                                final String message = "StorageException while trying to save the photo";
                                LOG.error("BodyTrackController.handlePhotoUpload(): " + message, e);
                                response = jsonResponseHelper.internalServerError(message);
                            }
                        }
                        else {
                            final String message = "Upload failed because both the 'photo' and 'metadata' parts are required and must be non-null";
                            LOG.error("BodyTrackController.handlePhotoUpload(): " + message);
                            response = jsonResponseHelper.badRequest(message);
                        }
                    }
                    else {
                        final String message = "Upload failed because the Multipart was null";
                        LOG.error("BodyTrackController.handlePhotoUpload(): " + message);
                        response = jsonResponseHelper.badRequest(message);
                    }
                }
                catch (Exception e) {
                    final String message = "Upload failed due to an unexpected exception";
                    LOG.error("BodyTrackController.handlePhotoUpload(): " + message, e);
                    response = jsonResponseHelper.internalServerError(message);
                }
            }
            else {
                final String message = "Upload failed because photo uploads are currently only allowed for the Fluxtream Capture connector";
                LOG.error("BodyTrackController.handlePhotoUpload(): " + message);
                response = jsonResponseHelper.badRequest(message);
            }
        }
        else {
            final String message = "Upload failed because the connector [" + connectorName + "] is unknown";
            LOG.error("BodyTrackController.handlePhotoUpload(): " + message);
            response = jsonResponseHelper.badRequest(message);
        }

        return response;
    }

    @GET
    @Path("/photo/{UID}.{PhotoStoreKeySuffix}")
    @ApiOperation(value="Retrieve a specific photo by photo store key suffix")
    @Deprecated
    public Response getFluxtreamCapturePhoto(@ApiParam(value="User ID", required=true) @PathParam("UID") final Long uid,
                                             @ApiParam(value="Photo Store Key Suffix", required=true) @PathParam("PhotoStoreKeySuffix") final String photoStoreKeySuffix,
                                             @Context final Request request) {

        return getFluxtreamCapturePhoto(uid, request, new FluxtreamCapturePhotoFetchStrategy() {
            private final String photoStoreKey = uid + "." + photoStoreKeySuffix;

            @Nullable
            @Override
            public FluxtreamCapturePhotoStore.Photo getPhoto() throws FluxtreamCapturePhotoStore.StorageException {
                return fluxtreamCapturePhotoStore.getPhoto(photoStoreKey);
            }

            @NotNull
            @Override
            public String getPhotoIdentifier() {
                return photoStoreKey;
            }
        });
    }

    @GET
    @Path("/photoThumbnail/{UID}/{PhotoId}/{ThumbnailIndex}")
    @ApiOperation(value="Retrieve a specific photo thumbnail")
    @Deprecated
    public Response getFluxtreamCapturePhotoThumbnail(@ApiParam(value="User ID", required=true) @PathParam("UID") final long uid,
                                                      @ApiParam(value="Photo ID", required=true) @PathParam("PhotoId") final long photoId,
                                                      @ApiParam(value="Thumbnail ID", required=true) @PathParam("ThumbnailIndex") final int thumbnailIndex,
                                                      @Context final Request request) {

        return getFluxtreamCapturePhoto(uid, request, new FluxtreamCapturePhotoFetchStrategy() {
            @Nullable
            @Override
            public FluxtreamCapturePhotoStore.Photo getPhoto() {
                return fluxtreamCapturePhotoStore.getPhotoThumbnail(uid, photoId, thumbnailIndex);
            }

            @NotNull
            @Override
            public String getPhotoIdentifier() {
                return uid + "/" + photoId + "/" + thumbnailIndex;
            }
        });
    }

    // Based on code from http://stackoverflow.com/questions/3496209/input-and-output-binary-streams-using-jersey/12573173#12573173
    private Response getFluxtreamCapturePhoto(final long uid,
                                              final Request request,
                                              @NotNull final FluxtreamCapturePhotoFetchStrategy photoFetchStrategy) {

        // Check authorization: is the logged-in user the same as the UID in the key?  If not, does the logged-in user
        // have coaching access AND access to the FluxtreamCapture connector?
        boolean accessAllowed = false;
        Long loggedInUserId = null;
        try {
            loggedInUserId = AuthHelper.getGuestId();
            accessAllowed = checkForPermissionAccess(uid);
            if (!accessAllowed) {
                final TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);
                if (trustedBuddy != null) {
                    accessAllowed = trustedBuddy.hasAccessToConnector("fluxtream_capture");
                }
            }
        }
        catch (Exception e) {
            LOG.error("BodyTrackController.getFluxtreamCapturePhoto(): Exception while trying to check authorization.", e);
        }

        if (accessAllowed) {

            final FluxtreamCapturePhotoStore.Photo photo;
            try {
                photo = photoFetchStrategy.getPhoto();
            }
            catch (Exception e) {
                final String message = "Exception while trying to get photo [" + photoFetchStrategy.getPhotoIdentifier() + "]";
                LOG.error("BodyTrackController.getFluxtreamCapturePhoto(): " + message, e);
                return jsonResponseHelper.internalServerError(message);
            }

            if (photo == null) {
                final String message = "Photo [" + photoFetchStrategy.getPhotoIdentifier() + "] requested by user [" + loggedInUserId + "] not found";
                LOG.error("BodyTrackController.getFluxtreamCapturePhoto(): " + message);
                return jsonResponseHelper.notFound(message);
            }

            final CacheControl cc = new CacheControl();
            cc.setNoTransform(true);
            cc.setMustRevalidate(false);
            cc.setNoCache(false);
            cc.setMaxAge(ONE_WEEK_IN_SECONDS);

            EntityTag etag;
            try {
                etag = new EntityTag(HashUtils.computeMd5Hash(photo.getPhotoBytes()));

                final Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);
                if (responseBuilder != null) {
                    // Preconditions are not met, returning HTTP 304 'not-modified'
                    return responseBuilder.cacheControl(cc).build();
                }
            }
            catch (NoSuchAlgorithmException e) {
                LOG.warn("NoSuchAlgorithmException caught while trying to create an MD5 hash for photo [" + photo.getIdentifier() + "].  No Etag will be specified in the response.");
                etag = null;
            }

            // Start building the response
            Response.ResponseBuilder responseBuilder = Response.ok().cacheControl(cc);
            if (etag != null) {
                responseBuilder = responseBuilder.tag(etag);
            }

            // Add the Last Modified header to the response, if we know it
            final Long lastUpdatedTimestamp = photo.getLastUpdatedTimestamp();
            if (lastUpdatedTimestamp != null) {
                responseBuilder = responseBuilder.lastModified(new Date(lastUpdatedTimestamp));
            }

            return responseBuilder
                    .type(photo.getImageType().getMediaType())
                    .expires(new DateTime().plusMonths(1).toDate())
                    .entity(photo.getPhotoBytes()).build();
        }

        return jsonResponseHelper.forbidden("User [" + loggedInUserId + "] is not authorized to view photo [" + photoFetchStrategy.getPhotoIdentifier() + "]");
    }

    @GET
    @Path("/tiles/{UID}/{DeviceNickname}.{ChannelName}/{Level}.{Offset}.json")
    @ApiOperation(value="Get data tile for a given channel", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String fetchTile(@ApiParam(value="User ID", required=true) @PathParam("UID") Long uid,
                            @ApiParam(value="Device Name", required=true) @PathParam("DeviceNickname") String deviceNickname,
                            @ApiParam(value="Channel Name", required=true) @PathParam("ChannelName") String channelName,
                            @ApiParam(value="Level of tile", required=true) @PathParam("Level") int level,
                            @ApiParam(value="Offset of tile", required=true) @PathParam("Offset") long offset){
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);
            if (!accessAllowed&& trustedBuddy ==null){
                uid = null;
            }
            return bodyTrackHelper.fetchTile(uid, deviceNickname, channelName, level, offset);
        } catch (Exception e){
            return gson.toJson(new StatusModel(false, "Access Denied"));
        }
    }

    @GET
    @Path("/users/{UID}/views")
    @ApiOperation(value="Get a list of available views", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String getViews(@ApiParam(value="User ID", required=true) @PathParam("UID") Long uid) {
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);
            if (!accessAllowed&& trustedBuddy ==null){
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
    @ApiOperation(value="Retrieve a specific view", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String bodyTrackView(@ApiParam(value="User ID", required=true) @PathParam("UID") Long uid,
                                @ApiParam(value="View ID", required= true) @PathParam("id") long id) {
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            if (!accessAllowed && trustedBuddy ==null) {
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
    @ApiOperation(value="Create a new view with given name and data", response = String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String setView(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid,
                          @ApiParam(value="View name", required = true) @FormParam("name") String name,
                          @ApiParam(value="View data", required = true)  @FormParam("data") String data) {
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            if (!accessAllowed && trustedBuddy ==null) {
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
    @ApiOperation(value="Retrieves a list of devices and channels that data can be retrieved from", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String getSourceList(@ApiParam(value= "User ID", required= true) @PathParam("UID") Long uid) {
        try{
            final long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = null;
            if (!accessAllowed) {
                trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);
                accessAllowed = (trustedBuddy !=null);
            }
            if (!accessAllowed){
                uid = null;
            }
            return bodyTrackHelper.listSources(uid, trustedBuddy);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path(value = "/users/{UID}/sources/{source}/default_graph_specs")
    @ApiOperation(value = "Retrieves the default grapher settings for a device", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String bodyTrackGetDefaultGraphSpecs(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid,
                                                @ApiParam(value="Device name", required=true) @PathParam("source") String name) {
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            if (!accessAllowed && trustedBuddy ==null) {
                uid = null;
            }
            return bodyTrackHelper.getSourceInfo(uid, name);
        }
        catch (Exception e){
            return gson.toJson(new StatusModel(false,"Access Denied"));
        }
    }

    @GET
    @Path(value = "/users/{UID}/tags")
    @ApiOperation(value = "Retrieve all tags for a user", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String getAllTagsForUser(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid) {
        try {
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);
            if (!accessAllowed && trustedBuddy == null) {
                uid = null;
            }
            return bodyTrackHelper.getAllTagsForUser(uid);
        }
        catch (Exception e) {
            return gson.toJson(new StatusModel(false, "Access Denied"));
        }
    }

    @POST
    @Path("/users/{UID}/channels/{DeviceNickname}.{ChannelName}/set")
    @ApiOperation(value = "Set the default style for a channel", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String setDefaultStyle(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid,
                                  @ApiParam(value="Device name", required = true) @PathParam("DeviceNickname") String deviceNickname,
                                  @ApiParam(value="Channel name", required = true) @PathParam("ChannelName") String channelName,
                                  @ApiParam(value="Style data", required = true) @FormParam("user_default_style") String style) {
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
    @Path("/timespans/{UID}/{ConnectorName}.{ObjectTypeName}/{Level}.{Offset}.json")
    @ApiOperation(value = "Retrieve a timespan tile", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String fetchTimespanTile(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid,
                                    @ApiParam(value="Connector Name", required = true) @PathParam("ConnectorName") String connectorName,
                                    @ApiParam(value="Object Type Name", required = true) @PathParam("ObjectTypeName") String objectTypeName,
                                    @ApiParam(value="Tile level", required = true) @PathParam("Level") int level,
                                    @ApiParam(value="Tile offset", required = true) @PathParam("Offset") long offset) {
        try{
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            if (!accessAllowed && trustedBuddy ==null) {
                uid = null;
            }

            if (uid == null) {
                return gson.toJson(new StatusModel(false, "Invalid User ID (null)"));
            }

            List<ApiKey> keys = guestService.getApiKeys(uid);
            ApiKey api = null;

            for (ApiKey key : keys){
                Connector connector = key.getConnector();
                if (connector.getName().equals(connectorName)||
                    connector.getPrettyName().equals(connectorName)){
                    api = key;
                    break;
                }
            }

            if (api == null) {
                return gson.toJson(new StatusModel(false, "Invalid Channel (null)"));
            }


            final long startTimeMillis = (long)(LevelOffsetHelper.offsetAtLevelToUnixTime(level, offset) * 1000);
            final long endTimeMillis = (long)(LevelOffsetHelper.offsetAtLevelToUnixTime(level, offset + 1) * 1000);

            final AbstractBodytrackResponder bodytrackResponder = api.getConnector().getBodytrackResponder(beanFactory);
            final List<TimespanModel> timespans = bodytrackResponder.getTimespans(startTimeMillis, endTimeMillis, api, objectTypeName);
            TimespanTileResponse response = new TimespanTileResponse(timespans);
            return gson.toJson(response);

        }
        catch (Exception e) {
            LOG.error("BodyTrackController.fetchTimespanTile(): Exception while trying to fetch timespans: ", e);
            return gson.toJson(new StatusModel(false, "Access Denied"));
        }

    }

    private class TimespanTileResponse{

        List<TimespanModel> data = new ArrayList<TimespanModel>();
        String type = "timespan";

        public TimespanTileResponse(List<TimespanModel> data){
            this.data = data;
        }
    }


    @GET
    @Path("/photos/{UID}/{ConnectorPrettyName}.{ObjectTypeName}/{Level}.{Offset}.json")
    @ApiOperation(value = "Retrieve a photo tile", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String fetchPhotoTile(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid,
                                 @ApiParam(value="Connector name", required = true) @PathParam("ConnectorPrettyName") String connectorPrettyName,
                                 @ApiParam(value="Object type name", required = true) @PathParam("ObjectTypeName") String objectTypeName,
                                 @ApiParam(value="Tile level", required = true) @PathParam("Level") int level,
                                 @ApiParam(value="Tile offset", required = true) @PathParam("Offset") long offset,
                                 @ApiParam(value="Tags for filtering", required = true) @QueryParam("tags") String tagsStr,
                                 @ApiParam(value="Tag matching strategy", required = true) @QueryParam("tag-match") String tagMatchingStrategyName) {
        try {
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            final TagFilter.FilteringStrategy tagFilteringStrategy = TagFilter.FilteringStrategy.findByName(tagMatchingStrategyName);

            if (!accessAllowed && trustedBuddy ==null) {
                uid = null;
            }

            if (uid == null) {
                return gson.toJson(new StatusModel(false, "Invalid User ID (null)"));
            }

            // first use Level and Offset to calculate the desired start and end times
            final long startTimeMillis = (long)(LevelOffsetHelper.offsetAtLevelToUnixTime(level, offset) * 1000);
            final long endTimeMillis = (long)(LevelOffsetHelper.offsetAtLevelToUnixTime(level, offset + 1) * 1000);

            final TimeInterval timeInterval = new SimpleTimeInterval(startTimeMillis, endTimeMillis, TimeUnit.ARBITRARY, TimeZone.getTimeZone("UTC"));

            // fetch the photos for this time interval, and for the desired device/channel
            final TagFilter tagFilter = TagFilter.create(Tag.parseTagsIntoStrings(tagsStr, Tag.COMMA_DELIMITER), tagFilteringStrategy);
            final SortedSet<PhotoService.Photo> photos = photoService.getPhotos(uid, timeInterval, connectorPrettyName, objectTypeName, tagFilter);

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

            if (LOG_DEBUG.isDebugEnabled()) {
                LOG_DEBUG.debug("BodyTrackController.fetchPhotoTile(): num photos filtered from " + photos.size() + " to " + filteredPhotos.size());
            }

            return gson.toJson(filteredPhotos);
        }
        catch (Exception e) {
            LOG.error("BodyTrackController.fetchPhotoTile(): Exception while trying to fetch photos: ", e);
            return gson.toJson(new StatusModel(false, "Access Denied"));
        }
    }

    @GET
    @Path("/photos/{UID}/{ConnectorPrettyName}.{ObjectTypeName}/{unixTime}/{count}")
    @ApiOperation(value="Get photos at a given time", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public String getPhotosBeforeOrAfterTime(@ApiParam(value="User ID", required = true) @PathParam("UID") long uid,
                                             @ApiParam(value="Connector name", required = true) @PathParam("ConnectorPrettyName") String connectorPrettyName,
                                             @ApiParam(value="Object type name", required = true) @PathParam("ObjectTypeName") String objectTypeName,
                                             @ApiParam(value="Timestamp (epoch seconds)", required = true) @PathParam("unixTime") double unixTimeInSecs,
                                             @ApiParam(value="Photo count limit", required = true) @PathParam("count") int desiredCount,
                                             @ApiParam(value="Is before time", required = true) @QueryParam("isBefore") boolean isGetPhotosBeforeTime,
                                             @ApiParam(value="Tags for matching", required = true) @QueryParam("tags") String tagsStr,
                                             @ApiParam(value="Tag matching strategy", required = true) @QueryParam("tag-match") String tagMatchingStrategyName) {
        try {
            long loggedInUserId = AuthHelper.getGuestId();
            boolean accessAllowed = checkForPermissionAccess(uid);
            TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);

            final TagFilter.FilteringStrategy tagFilteringStrategy = TagFilter.FilteringStrategy.findByName(tagMatchingStrategyName);

            if (!accessAllowed && trustedBuddy ==null) {
                return gson.toJson(new StatusModel(false, "Invalid User ID (null)"));
             }

            final TagFilter tagFilter = TagFilter.create(Tag.parseTagsIntoStrings(tagsStr, Tag.COMMA_DELIMITER), tagFilteringStrategy);
            final SortedSet<PhotoService.Photo> photos = photoService.getPhotos(uid, (long)(unixTimeInSecs * 1000), connectorPrettyName, objectTypeName, desiredCount, isGetPhotosBeforeTime, tagFilter);

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

    @GET
    @Path("/metadata/{UID}/{ConnectorName}.{ObjectTypeName}/{facetId}/get")
    @ApiOperation(value="Get the metadata for a facet", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public Response getFacetMetadata(@ApiParam(value="User ID", required = true) @PathParam("UID") Long uid,
                                     @ApiParam(value="Connector name", required = true) final @PathParam("ConnectorName") String connectorName,
                                     @ApiParam(value="Object type name", required = true) final @PathParam("ObjectTypeName") String objectTypeName,
                                     @ApiParam(value="Facet ID", required = true) final @PathParam("facetId") long facetId) {

        return executeFacetMetaDataOperation(uid, connectorName, objectTypeName, facetId, new FacetMetaDataOperation() {
            @Override
            @NotNull
            public Response executeOperation(@NotNull final AbstractFacet facet) {
                return Response.status(Response.Status.OK).entity(gson.toJson(new FacetMetadata(facet))).type(MediaType.APPLICATION_JSON).build();
            }
        });
    }

    @POST
    @Path("/metadata/{UID}/{ConnectorName}.{ObjectTypeName}/{facetId}/set")
    @ApiOperation(value="Set the metadata for a facet", response=String.class)
    @Produces({MediaType.APPLICATION_JSON})
    @Deprecated
    public Response setFacetMetadata(@ApiParam(value="User ID", required = true) final @PathParam("UID") long uid,
                                     @ApiParam(value="Connector name", required = true) final @PathParam("ConnectorName") String connectorName,
                                     @ApiParam(value="Object type name", required = true) final @PathParam("ObjectTypeName") String objectTypeName,
                                     @ApiParam(value="Facet ID", required = true) final @PathParam("facetId") long facetId,
                                     @ApiParam(value="Comment", required = true) final @FormParam("comment") String comment,
                                     @ApiParam(value="Tags", required = true) final @FormParam("tags") String tags) {

        // don't bother doing anything if comment and tags are both null
        if (comment != null || tags != null) {
            return executeFacetMetaDataOperation(uid, connectorName, objectTypeName, facetId, new FacetMetaDataOperation() {
                @Override
                @NotNull
                public Response executeOperation(@NotNull final AbstractFacet facet) throws Exception {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("BodyTrackController.setFacetMetadata(): Attempting to set metadata for facet [" + facetId + "] for connector [" + connectorName + "] and object type [" + objectTypeName + "]");
                    }

                    final FacetMetadataModifier facetMetadataModifier = new FacetMetadataModifier(uid, facetId, comment, tags);
                    final AbstractFacet modifiedFacet = apiDataService.createOrReadModifyWrite(facet.getClass(), facetMetadataModifier.getFacetFinderQuery(), facetMetadataModifier, facet.apiKeyId);

                    if (modifiedFacet != null) {
                        return jsonResponseHelper.ok("Metadata updated successfully!", new FacetMetadata(modifiedFacet));
                    }

                    return jsonResponseHelper.forbidden("User [" + uid + "] is not allowed to set metadata for facet [" + facetId + "] for connector [" + connectorName + "] and object type [" + objectTypeName + "]");
                }
            });
        }
        return jsonResponseHelper.badRequest("Nothing changed since comment and tags were both null");
    }

    private static class FacetMetadata {
        private String comment;
        private SortedSet<String> tags = new TreeSet<String>();

        private FacetMetadata(@NotNull AbstractFacet facet) {
            this.comment = facet.comment;
            this.tags.addAll(facet.getTagsAsStrings());
        }
    }

    private static interface FacetMetaDataOperation {
        @NotNull
        Response executeOperation(@NotNull final AbstractFacet facet) throws Exception;
    }

    private Response executeFacetMetaDataOperation(final long uid,
                                                   final String connectorName,
                                                   final String objectTypeName,
                                                   final long facetId,
                                                   final FacetMetaDataOperation operation) {
            // Try to find the connector by pretty name, and then if that fails the find by actual name
            Connector connector = ConnectorUtils.findConnectorByPrettyName(guestService, uid, connectorName);
            if (connector == null) {
                connector = Connector.getConnector(connectorName);
            }

            if (connector != null) {
                // Check authorization: is the logged-in user the same as the UID in the key?  If not, does the logged-in user
                // have coaching access AND access to the FluxtreamCapture connector?
                boolean accessAllowed = false;
                Long loggedInUserId = null;
                try {
                    loggedInUserId = AuthHelper.getGuestId();
                    accessAllowed = checkForPermissionAccess(uid);
                    if (!accessAllowed) {
                        final TrustedBuddy trustedBuddy = buddiesService.getTrustedBuddy(loggedInUserId, uid);
                        if (trustedBuddy != null) {
                            accessAllowed = trustedBuddy.hasAccessToConnector(connector.getName());
                        }
                    }
                }
                catch (Exception e) {
                    LOG.error("BodyTrackController.setFacetMetadata(): Exception while trying to check authorization.", e);
                }

                if (accessAllowed) {

                    final ObjectType objectType = ObjectType.getObjectType(connector, objectTypeName);
                    if (objectType != null) {
                        ApiKey apiKey = guestService.getApiKey(uid, connector);
                        final AbstractFacet facet = apiDataService.getFacetById(apiKey, objectType, facetId);
                        if (facet != null) {
                            try {
                                return operation.executeOperation(facet);
                            }
                            catch (Exception e) {
                                final String message = "Unexpected error while trying to operate on metadata for facet [" + facetId + "] for connector [" + connectorName + "] and object type [" + objectType + "]";
                                LOG_DEBUG.error(message, e);
                                return jsonResponseHelper.internalServerError(message);
                            }
                        }
                        return jsonResponseHelper.notFound("Unknown facet [" + facetId + "] for connector [" + connectorName + "] and object type [" + objectType + "] and guestId [" + uid + "]");
                    }
                    return jsonResponseHelper.notFound("Unknown object type [" + objectTypeName + "] for connector [" + connectorName + "]");
                }
                return jsonResponseHelper.forbidden("User [" + loggedInUserId + "] is not authorized to access or modify metadata for facets owned by user [" + uid + "] in connector [" + connectorName + "]");
            }
            return jsonResponseHelper.notFound("Unknown connector [" + connectorName + "]");
    }

    private static final class FacetMetadataModifier implements ApiDataService.FacetModifier<AbstractFacet> {
        @NotNull
        private final ApiDataService.FacetQuery facetFinderQuery;
        @Nullable
        private final String comment;
        @Nullable
        private final String tagsStr;

        public FacetMetadataModifier(final long guestId, final long facetId, @Nullable final String comment, @Nullable final String tagsStr) {
            this.comment = comment;
            this.tagsStr = tagsStr;
            facetFinderQuery = new ApiDataService.FacetQuery("e.id = ? and e.guestId = ?", facetId, guestId);
        }

        @Override
        public AbstractFacet createOrModify(final AbstractFacet existingFacet, final Long apiKeyId) {
            // the case where the existing facet doesn't exist and is null should never happen here
            if (existingFacet != null) {
                if (comment != null) {
                    existingFacet.comment = comment;
                }

                if (tagsStr != null) {
                    existingFacet.clearTags();
                    existingFacet.addTags(tagsStr, Tag.COMMA_DELIMITER);
                }
            }

            return existingFacet;
        }

        @NotNull
        public ApiDataService.FacetQuery getFacetFinderQuery() {
            return facetFinderQuery;
        }
    }

    private boolean checkForPermissionAccess(long targetUid){
        Guest guest = AuthHelper.getGuest();
        return targetUid == guest.getId() || guest.hasRole(Guest.ROLE_ADMIN);
    }

    private static class PhotoItem {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

        boolean nsfw = false;
        String id;
        String description;
        String comment;
        double begin_d;
        String begin;
        double end_d;
        String end;
        String dev_id;
        String dev_nickname;
        String object_type_name;
        String channel_name;
        String url;
        ArrayList<String> tags = new ArrayList<String>();
        ArrayList<PhotoItemThumbnail> thumbnails = new ArrayList<PhotoItemThumbnail>();
        int count = 1;
        int orientation;
        String time_type;

        public PhotoItem(final PhotoService.Photo photo) {
            final AbstractPhotoFacetVO photoFacetVO = photo.getAbstractPhotoFacetVO();

            this.id = photo.getConnector().prettyName() + "." + photo.getObjectType().getName() + "." + photoFacetVO.id;
            this.description = photoFacetVO.description == null ? "" : photoFacetVO.description;
            this.comment = photoFacetVO.comment == null ? "" : photoFacetVO.comment;
            this.begin_d = photoFacetVO.start / 1000.0; // convert millis to seconds
            this.begin = DATE_TIME_FORMATTER.print(photoFacetVO.start);
            this.end_d = this.begin_d;
            this.end = this.begin;
            this.dev_id = photo.getConnector().getName();
            this.dev_nickname = photo.getConnector().prettyName();
            this.object_type_name = photo.getObjectType().getName();
            this.time_type = photoFacetVO.timeType;
            this.channel_name = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;   // photo channels are always named the same
            final List<DimensionModel> thumbnailSizes = photoFacetVO.getThumbnailSizes();
            if ((thumbnailSizes != null) && (!thumbnailSizes.isEmpty())) {
                int i = 0;
                for (final DimensionModel thumbnailDimension : thumbnailSizes) {
                    final String url = photoFacetVO.getThumbnail(i);
                    thumbnails.add(new PhotoItemThumbnail(url, thumbnailDimension.width, thumbnailDimension.height));
                    i++;
                }
            }

            this.url = photoFacetVO.getPhotoUrl();

            // copy the tags
            final SortedSet<String> facetTags = photoFacetVO.getTags();
            if ((facetTags != null) && (!facetTags.isEmpty())) {
                this.tags.addAll(facetTags);
            }

            // get the image orientation, defaulting to upright portrait
            final ImageOrientation tempOrientation = photoFacetVO.getOrientation();
            this.orientation = (tempOrientation == null ? ImageOrientation.ORIENTATION_1 : tempOrientation).getId();
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

    private static final class BodyTrackUploadResponse {
        // We only store the bare minimum here because it might be a security/privacy issue to include everything (Randy
        // explained to Chris on 2012.10.31 that we probably don't want to make channel ranges and such visible by
        // default.  Plus, if debugging is on in the datastore, file paths might also be included in the response JSON).
        String successful_records;
        String failed_records;
        String failure;
    }

    private class PhotoUploadResponsePayload {
        @NotNull
        @Expose
        private final String operation;

        @NotNull
        @Expose
        private final String key;

        @Nullable
        @Expose
        private final Long id;

        public PhotoUploadResponsePayload(@NotNull final FluxtreamCapturePhotoStore.Operation operation, @Nullable final Long databaseRecordId, @NotNull final String photoStoreKey) {
            this.id = databaseRecordId;
            this.operation = operation.getName();
            this.key = photoStoreKey;
        }
    }

    private interface FluxtreamCapturePhotoFetchStrategy {

        @Nullable
        FluxtreamCapturePhotoStore.Photo getPhoto() throws FluxtreamCapturePhotoStore.StorageException;

        @NotNull
        String getPhotoIdentifier();
    }
}
