package org.fluxtream.core.services.impl;

import com.google.gson.*;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.fluxtream.core.Configuration;
import org.fluxtream.core.TimeInterval;
import org.fluxtream.core.aspects.FlxLogger;
import org.fluxtream.core.auth.AuthHelper;
import org.fluxtream.core.connectors.Connector;
import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.*;
import org.fluxtream.core.services.*;
import org.fluxtream.core.utils.JPAUtils;
import org.fluxtream.core.utils.Utils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

//import java.nio.file.Path;
//import java.nio.file.Paths;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
@Transactional(readOnly = true)
public class BodyTrackHelper {

    public interface BodyTrackUploadResult {
        /**
         * Status code for the upload operation.
         * @see #isSuccess()
         */
        int getStatusCode();

        /** Text output from the upload, usually (always?) JSON. */
        String getResponse();

        /** Returns <code>true</code> if the upload was successful, <code>false</code> otherwise. */
        boolean isSuccess();
    }

    protected long getMinTimeForApiKey(long apiKeyId, Integer objectTypeId){
        ApiKey apiKey = guestService.getApiKey(apiKeyId);

        ObjectType[] objectTypes;
        if (objectTypeId != null)
            objectTypes = apiKey.getConnector().getObjectTypesForValue(objectTypeId);
        else
            objectTypes = apiKey.getConnector().objectTypes();
        if (objectTypes == null || objectTypes.length == 0){
            final String minTimeAtt = guestService.getApiKeyAttribute(apiKey, ApiKeyAttribute.MIN_TIME_KEY);
            if (minTimeAtt !=null && StringUtils.isNotEmpty(minTimeAtt)) {
                final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(minTimeAtt);
                return dateTime.getMillis();
            }
        }
        else{
            long minTime = Long.MAX_VALUE;
            for (ObjectType objectType : objectTypes){
                final String minTimeAtt = guestService.getApiKeyAttribute(apiKey, objectType.getApiKeyAttributeName(ApiKeyAttribute.MIN_TIME_KEY));
                if (minTimeAtt !=null && StringUtils.isNotEmpty(minTimeAtt)) {
                    final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(minTimeAtt);
                    minTime = Math.min(minTime, dateTime.getMillis());
                }

            }
            if (minTime < Long.MAX_VALUE)
                return minTime;
        }
        //if we couldn't get the minTime from ApiKey Attributes fallback to oldest facet
        AbstractFacet facet;
        if (objectTypes == null || objectTypes.length == 0){
            facet = apiDataService.getOldestApiDataFacet(apiKey,null);
        }
        else{
            facet = null;
            for (ObjectType objectType : objectTypes){
                AbstractFacet potentialFacet = apiDataService.getOldestApiDataFacet(apiKey,objectType);
                if (potentialFacet!=null) {
                    if (facet == null || facet.start > potentialFacet.start)
                        facet = potentialFacet;
                }
            }
        }
        if (facet != null)
            return facet.start;
        else
            return Long.MAX_VALUE;
    }

    protected long getMaxTimeForApiKey(long apiKeyId, Integer objectTypesMask){
        ApiKey apiKey = guestService.getApiKey(apiKeyId);

        ObjectType[] objectTypes;
        if (objectTypesMask != null)
            objectTypes = apiKey.getConnector().getObjectTypesForValue(objectTypesMask);
        else
            objectTypes = apiKey.getConnector().objectTypes();
        if (objectTypes == null || objectTypes.length == 0){
            final String maxTimeAtt = guestService.getApiKeyAttribute(apiKey, ApiKeyAttribute.MAX_TIME_KEY);
            if (maxTimeAtt !=null && StringUtils.isNotEmpty(maxTimeAtt)) {
                final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(maxTimeAtt);
                return dateTime.getMillis();
            }
        }
        else{
            long maxTime = Long.MIN_VALUE;
            for (ObjectType objectType : objectTypes){
                final String maxTimeAtt= guestService.getApiKeyAttribute(apiKey, objectType.getApiKeyAttributeName(ApiKeyAttribute.MAX_TIME_KEY));
                if (maxTimeAtt !=null && StringUtils.isNotEmpty(maxTimeAtt)) {
                    final DateTime dateTime = ISODateTimeFormat.dateHourMinuteSecondFraction().withZoneUTC().parseDateTime(maxTimeAtt);
                    maxTime = Math.max(maxTime,dateTime.getMillis());
                }

            }
            if (maxTime > Long.MIN_VALUE)
                return maxTime;
        }
        //if we couldn't get the minTime from ApiKey Attributes fallback to oldest facet
        AbstractFacet facet;
        if (objectTypes == null || objectTypes.length == 0){
            facet = apiDataService.getLatestApiDataFacet(apiKey,null);
        }
        else{
            facet = null;
            for (ObjectType objectType : objectTypes){
                AbstractFacet potentialFacet = apiDataService.getLatestApiDataFacet(apiKey,objectType);
                if (potentialFacet != null && (facet == null || facet.end < potentialFacet.end))
                    facet = potentialFacet;
            }
        }
        if (facet != null)
            return facet.end;
        else
            return Long.MIN_VALUE;
    }

    public void setChannelBounds(final ChannelMapping mapping, final Channel channel, ChannelInfoResponse infoResponse) {
        Set<String> channelNames = infoResponse.channel_specs.keySet();
        boolean datastoreChannelBoundsSet = false;
        switch (mapping.getChannelType()){
            case photo:
                channel.min = 0.6;
                channel.max = 1;
                break;
            case data:
                Connector connector = Connector.fromDeviceNickname(mapping.getDeviceName());
                if (connector!=null) {
                    // historically, some channels have used a device nickname, others a connector name
                    String deviceChannelName = new StringBuilder(connector.getName()).append(".").append(mapping.getChannelName()).toString();
                    String deviceNicknameChannelName = new StringBuilder(mapping.getDeviceName()).append(".").append(mapping.getChannelName()).toString();
                    String internalDeviceNicknameChannelName = new StringBuilder(mapping.getInternalDeviceName()).append(".").append(mapping.getChannelName()).toString();
                    String deviceInternalChannelName = new StringBuilder(mapping.getDeviceName()).append(".").append(mapping.getInternalChannelName()).toString();
                    for (String channelName : channelNames) {
                        if (channelName.toLowerCase().equals(deviceChannelName.toLowerCase()) || channelName.toLowerCase().equals(deviceNicknameChannelName.toLowerCase())||
                                channelName.toLowerCase().equals(internalDeviceNicknameChannelName.toLowerCase())||deviceInternalChannelName.equals(channelName)) {
                            ChannelSpecs channelSpecs = infoResponse.channel_specs.get(channelName);
                            channel.min = channelSpecs.channel_bounds.min_value;
                            channel.max = channelSpecs.channel_bounds.max_value;
                            channel.min_time = channelSpecs.channel_bounds.min_time;
                            channel.max_time = channelSpecs.channel_bounds.max_time;
                            datastoreChannelBoundsSet = true;
                            break;
                        }
                    }
                } else {
                    channel.min = 0;
                    channel.max = 1;
                }
                break;
            default:
                channel.min = 0;
                channel.max = 1;
                break;
        }
        if (!datastoreChannelBoundsSet) {
            long maxTime = getMaxTimeForApiKey(mapping.getApiKeyId(), mapping.getObjectTypes());
            long minTime = getMinTimeForApiKey(mapping.getApiKeyId(), mapping.getObjectTypes());
            if (maxTime < minTime) {
                channel.max_time = 0d;
                channel.min_time = 0d;
            } else {
                channel.max_time = maxTime / 1000.0;
                channel.min_time = minTime / 1000.0;
            }
        }
    }

    @PersistenceContext
    EntityManager em;

    static final boolean verboseOutput = false;
    static final boolean showOutput = false;

    @Autowired
    DataUpdateService dataUpdateService;

    @Autowired
    Configuration env;

    @Autowired
    PhotoService photoService;

    @Autowired
    GuestService guestService;

    @Autowired
    ApiDataService apiDataService;

    @Autowired
    BuddiesService buddiesService;

    @Autowired
    BeanFactory beanFactory;

    // Create a Gson parser which handles ChannelBounds specially to avoid problems with +/- infinity
    Gson gson = new GsonBuilder().registerTypeAdapter(ChannelBounds.class, new ChannelBoundsDeserializer()).create();
    static FlxLogger logger = FlxLogger.getLogger(BodyTrackHelper.class);

    private int executeDataStore(String commandName, Object[] parameters,OutputStream out){
        try{
            Runtime rt = Runtime.getRuntime();
	    String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/" + commandName + " " +
		                   env.targetEnvironmentProps.getString("btdatastore.db.location");

	    //            Path commandPath= Paths.get(env.targetEnvironmentProps.getString("btdatastore.exec.location"));
	    //            Path launchExecutable = commandPath.resolve(commandName);
	    //            String launchCommand = launchExecutable.toString()+ " " + env.targetEnvironmentProps.getString("btdatastore.db.location");
            for (Object param : parameters){
                launchCommand += ' ';
                String part = param.toString();
                if (part.indexOf(' ') == -1){
                    launchCommand += part;
                }
                else{
                    launchCommand += "\"" + part + "\"";
                }
            }
            if (showOutput)
                System.out.println("BTDataStore: running with command: " + launchCommand);

            //create process for operation
            final Process pr = rt.exec(launchCommand);

            new Thread(){//outputs the errorstream
                public void run(){
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line=null;
                    try{
                        if (verboseOutput && showOutput){
                            while((line=error.readLine()) != null) { //output all console output from the execution
                                System.out.println("BTDataStore-error: " + line);
                            }
                        }
                        else
                            while (error.readLine() != null) {}
                    } catch(Exception ignored){}
                }

            }.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line;

            boolean first = true;

            while((line=input.readLine()) != null) { //output all console output from the execution
                if (showOutput)
                    System.out.println("BTDataStore: " + line);
                if (first){
                    first = false;
                }
                else{
                    out.write("\n".getBytes());
                }
                out.write(line.getBytes());
            }
            int exitValue = pr.waitFor();
            if (showOutput)
                System.out.println("BTDataStore: exited with code " + exitValue);
            return exitValue;
        }
        catch (Exception e){
            if (showOutput)
                System.out.println("BTDataStore: datastore execution failed!");
            throw new RuntimeException("Datastore execution failed");
        }
    }

    private DataStoreExecutionResult executeDataStore(String commandName, Object[] parameters){
        final StringBuilder responseBuilder = new StringBuilder();
        int result = executeDataStore(commandName,parameters,new OutputStream(){

            @Override
            public void write(final int b) throws IOException {
                responseBuilder.append((char) b);
            }
        });
        return new DataStoreExecutionResult(result,responseBuilder.toString());

    }


    //start and end are optional
    public int exportToCSV(final Long guestId, final Collection<String> channelNames, final Long start, final Long end, final OutputStream out){
        try{
            if (guestId == null)
                throw new IllegalArgumentException();
            if (channelNames == null || channelNames.size() == 0)
                throw new IllegalArgumentException();

            ArrayList<String> params = new ArrayList<String>();
            params.add("--csv");
            params.add("" + guestId);
            params.addAll(channelNames);
            if (start != null){
                params.add("--start");
                params.add("" + start);
            }
            if (end != null){
                params.add("--end");
                params.add("" + end);
            }
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("export",params.toArray(new String[]{}));
            out.write(dataStoreExecutionResult.getResponse().getBytes());
            return dataStoreExecutionResult.getStatusCode();
        }
        catch (Exception e){
            return -1;
        }

    }

    public BodyTrackUploadResult uploadToBodyTrack(final ApiKey apiKey,
                                  final String deviceName,
                                  final Collection<String> channelNames,
                                  final List<List<Object>> data) {
        try{
            if (apiKey == null)
                throw new IllegalArgumentException();
            final File tempFile = File.createTempFile("input",".json");

            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data", data);
            tempFileMapping.put("channel_names", channelNames);

            FileOutputStream fos = new FileOutputStream(tempFile);
            final String bodyTrackJSONData = gson.toJson(tempFileMapping);
            fos.write(bodyTrackJSONData.getBytes());
            fos.close();

            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("import", new Object[]{apiKey.getGuestId(), deviceName, tempFile.getAbsolutePath()});
            ParsedBodyTrackUploadResult parsedResult = new ParsedBodyTrackUploadResult(dataStoreExecutionResult, deviceName, gson);
            if (!dataStoreExecutionResult.isSuccess()) {
                logger.warn("Datastore: There was an error persisting data to the datastore, guestId: " + apiKey.getGuestId() + ", deviceName: " + deviceName + ", tempFile: " + tempFile.getCanonicalPath());
                dataUpdateService.logBodyTrackDataUpdate(apiKey.getGuestId(),
                        apiKey.getId(), null, deviceName, channelNames.toArray(new String[channelNames.size()]), dataStoreExecutionResult.getResponse());
            } else {
                try {
                    long startTime = 0, endTime = 0;
                    if (parsedResult.getParsedResponse().min_time!=null)
                        startTime = (long) (parsedResult.getParsedResponse().min_time * 1000);
                    if (parsedResult.getParsedResponse().max_time!=null)
                        endTime = (long) (parsedResult.getParsedResponse().max_time * 1000);
                    dataUpdateService.logBodyTrackDataUpdate(apiKey.getGuestId(),
                            apiKey.getId(), null, deviceName, channelNames.toArray(new String[channelNames.size()]), startTime, endTime);
                } catch (Throwable t) {
                    logger.warn("Datastore: couldn't log successful api data update");
                    logger.warn(ExceptionUtils.getStackTrace(t));
                }
            }
            tempFile.delete();
            return parsedResult;
        } catch (Exception e) {
            System.err.println("Could not persist to datastore");
            System.err.println(Utils.stackTrace(e));
            throw new RuntimeException("Could not persist to datastore");
        }
    }

    public BodyTrackUploadResult uploadJsonToBodyTrack(final Long guestId,
                                      final String deviceName,
                                      final String json) {
         try{
             if (guestId == null)
                 throw new IllegalArgumentException();
             final File tempFile = File.createTempFile("input",".json");

             FileOutputStream fos = new FileOutputStream(tempFile);
             fos.write(json.getBytes());
             fos.close();

             final ParsedBodyTrackUploadResult dataStoreExecutionResult = new ParsedBodyTrackUploadResult(executeDataStore("import", new Object[]{guestId, deviceName, tempFile.getAbsolutePath()}), deviceName, gson);
             tempFile.delete();
             if (dataStoreExecutionResult.isSuccess()){//log to DataUpdate table //TODO: confirm this works
                 List<ApiKey> keys = guestService.getApiKeys(guestId,Connector.getConnector("fluxtream_capture"));
                 long apiKeyId = -1;
                 if (keys.size() > 0){
                     apiKeyId = keys.get(0).getId();
                 }
                 dataUpdateService.logBodyTrackDataUpdate(guestId,apiKeyId,null,dataStoreExecutionResult);
             }
             return dataStoreExecutionResult;
         } catch (Exception e) {
             System.out.println("Could not persist to datastore");
             System.out.println(Utils.stackTrace(e));
             throw new RuntimeException("Could not persist to datastore");
         }
     }

    public GetTileResponse fetchTileObject(Long guestId, String deviceNickname, String channelName, int level, long offset){
        try{
            if (guestId == null)
                throw new IllegalArgumentException();
            ChannelMapping mapping = getChannelMapping(guestId, deviceNickname, channelName);
            String internalDeviceName = mapping != null ? mapping.getInternalDeviceName() : deviceNickname;
            String internalChannelName = mapping != null ? mapping.getInternalChannelName() : channelName;
            internalDeviceName = checkDatastoreDir(guestId, internalDeviceName);
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("gettile", new Object[]{guestId, internalDeviceName + "." + internalChannelName, level, offset});
            String result = dataStoreExecutionResult.getResponse();

            // TODO: check statusCode in DataStoreExecutionResult
            GetTileResponse tileResponse = gson.fromJson(result,GetTileResponse.class);

            if (tileResponse.data == null){
                tileResponse = GetTileResponse.getEmptyTile(level,offset);
            }//TODO:several fields are missing still and should be implemented

            return tileResponse;
        }
        catch(Exception e){
            return GetTileResponse.getEmptyTile(level,offset);
        }
    }

    private String checkDatastoreDir(Long guestId, String internalDeviceName) throws IOException {
        File dir = new File(env.targetEnvironmentProps.getString("btdatastore.db.location")+File.separator+guestId+File.separator+ internalDeviceName);
        if (dir.exists() && dir.getCanonicalPath().endsWith(internalDeviceName))
            return internalDeviceName;
        String connectorName = Connector.fromDeviceNickname(internalDeviceName).getName();
        dir = new File(env.targetEnvironmentProps.getString("btdatastore.db.location")+File.separator+guestId+File.separator+connectorName);
        if (dir.exists() && dir.getCanonicalPath().endsWith(connectorName))
            return connectorName;
        return internalDeviceName;
    }

    public String fetchTile(Long guestId, String deviceNickname, String channelName, int level, long offset){
        return gson.toJson(fetchTileObject(guestId,deviceNickname,channelName,level,offset));
    }

    public String getSourcesResponse(Long guestId, TrustedBuddy trustedBuddy) {
        final SourcesResponse response = new SourcesResponse();

        final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("info",new Object[]{"-r",guestId});
        String result = dataStoreExecutionResult.getResponse();

        // Iterate over the various (photo) connectors (if any), manually inserting each into the ChannelSpecs
        final Map<String, TimeInterval> photoChannelTimeRanges = photoService.getPhotoChannelTimeRanges(guestId, null);

        // TODO: check statusCode in DataStoreExecutionResult
        ChannelInfoResponse infoResponse = gson.fromJson(result, ChannelInfoResponse.class);

        // create the 'All' photos block
        final Source allPhotosSource = getAllPhotosSource(infoResponse, photoChannelTimeRanges);

        // retrieve channel mappings directly if trustedBuddy is null or through the SharedChannels otherwise
        final List<ChannelMapping> channelMappings = getChannelMappings(guestId, trustedBuddy);

        // populateResponseWithChannelMappings is meant to be backward compatible with the LegacyBodytrackController
        // and so it includes a trustedBuddy parameter because it has another (deprecated) way of figuring out
        // access permissions to a buddy's info - here it has to be null since we have already filtered out
        // Channels to which the loggedIn guest doesn't have access
        try {
            populateResponseWithChannelMappings(guestId, null /*IMPORTANT: trustedBuddy needs to be null here*/,
                    response, channelMappings, infoResponse);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected error trying to populate response with channel mappings: " + e.getMessage());
        }

        // if trustedBuddy is null, add the All photos block to the response
        if (trustedBuddy==null&&!photoChannelTimeRanges.isEmpty()) {
            response.sources.add(allPhotosSource);
        }

        final String jsonResponse = gson.toJson(response);
        return jsonResponse;
    }

    public String listSources(Long guestId, TrustedBuddy trustedBuddy){
        SourcesResponse response = null;
        try{
            if (guestId == null) {
                throw new IllegalArgumentException();
            }
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("info",new Object[]{"-r",guestId});
            String result = dataStoreExecutionResult.getResponse();

            // TODO: check statusCode in DataStoreExecutionResult
            ChannelInfoResponse infoResponse = gson.fromJson(result,ChannelInfoResponse.class);

            // Iterate over the various (photo) connectors (if any), manually inserting each into the ChannelSpecs
            final Map<String, TimeInterval> photoChannelTimeRanges = photoService.getPhotoChannelTimeRanges(guestId, trustedBuddy);

            // create the 'All' photos block
            final Source allPhotosSource = getAllPhotosSource(infoResponse, photoChannelTimeRanges);

            // create the respone
            response = new SourcesResponse(infoResponse, guestId, trustedBuddy);

            // filter out photo connectors that aren't shared with this user
            if (trustedBuddy !=null) {
                List<String> sourcesToRemove = new ArrayList<String>();
                for (Source source : response.sources) {
                    final Connector photoConnectorForSource = Connector.fromDeviceNickname(source.name);
                    if (photoConnectorForSource!=null) {
                        final List<ApiKey> apiKeys = guestService.getApiKeys(trustedBuddy.guestId, photoConnectorForSource);
                        for (ApiKey apiKey : apiKeys) {
                            if (buddiesService.getSharedConnector(apiKey.getId(), AuthHelper.getGuestId())==null) {
                                sourcesToRemove.add(source.name);
                                break;
                            }
                        }
                      // 09/15/2014 on Anne's request: until we have thoroughly fixed the management
                      // of channel mappings, sources that don't map to connectors need to continue being
                      // shared with buddies
//                    } else {
//                        // let's be conservative: if we don't know this connector, let's assume
//                        // it wasn't shared
//                        sourcesToRemove.add(source.name);
                    }
                }
                for (String sourceName : sourcesToRemove) {
                    response.deleteSource(sourceName);
                }
            }

            //TODO: this is a hack to prevent double flickr photo channel showing up
            response.deleteSource("Flickr");
            response.deleteSource("SMS_Backup");

            final List<ChannelMapping> channelMappings = getChannelMappings(guestId, trustedBuddy);
            populateResponseWithChannelMappings(guestId, trustedBuddy, response, channelMappings, infoResponse);

            // add the All photos block to the response
            if (!photoChannelTimeRanges.isEmpty()) {
                response.sources.add(allPhotosSource);
            }

            for (Source source : response.sources){
                if (source.max_time < source.min_time)
                    source.min_time = source.max_time = 0.0;
            }

            final String jsonResponse = gson.toJson(response);
            return jsonResponse;
        }
        catch(Exception e){
            e.printStackTrace();
            StringBuilder sb = new StringBuilder("module=bodytrackHelper component=listSources action=listSources")
                    .append(" guestId=")
                    .append(guestId)
                    .append(" message=").append(e.getMessage());

            if(response!=null) {
                // In case the exception was caused by speical floating point values such as
                // Infinity, create a gson builder that will potentially let us debug even though
                // the javascript would choke on the result if we returned it
                Gson errorGson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
                sb.append(" response=").append(errorGson.toJson(response));
            }

            logger.error(sb.toString());

            return gson.toJson(new SourcesResponse(null, guestId, trustedBuddy));
        }
    }

    private Source getAllPhotosSource(ChannelInfoResponse infoResponse, Map<String, TimeInterval> photoChannelTimeRanges) {
        final Source allPhotosSource = new Source();
        if (!photoChannelTimeRanges.isEmpty()) {
            allPhotosSource.name = PhotoService.ALL_DEVICES_NAME;
            allPhotosSource.channels = new ArrayList<Channel>();
            final Channel allPhotosChannel = new Channel();
            allPhotosSource.channels.add(allPhotosChannel);
            allPhotosChannel.name = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;   // photo channels are always named the same
            allPhotosChannel.objectTypeName = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;
            allPhotosChannel.type = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;
            allPhotosChannel.builtin_default_style = new ChannelStyle();
            allPhotosChannel.style = allPhotosChannel.builtin_default_style;
            allPhotosChannel.min = .6;
            allPhotosChannel.max = 1;
            allPhotosChannel.min_time = Double.MAX_VALUE;
            allPhotosChannel.max_time = Double.MIN_VALUE;

            final double defaultTimeForNullTimeIntervals = System.currentTimeMillis() / 1000;

            for (final String channelName : photoChannelTimeRanges.keySet()) {

                final ChannelSpecs channelSpecs = new ChannelSpecs();
                final TimeInterval timeInterval = photoChannelTimeRanges.get(channelName);

                // mark this channel as a photo channel so that the grapher can properly render it as a photo channel
                channelSpecs.channelType = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;
                final String[] connectorNameAndObjectTypeName = channelName.split("\\.");
                if (connectorNameAndObjectTypeName.length > 1) {
                    channelSpecs.objectTypeName = connectorNameAndObjectTypeName[1];
                }

                channelSpecs.channel_bounds = new ChannelBounds();
                if (timeInterval == null) {
                    channelSpecs.channel_bounds.min_time = defaultTimeForNullTimeIntervals;
                    channelSpecs.channel_bounds.max_time = defaultTimeForNullTimeIntervals;
                }
                else {
                    channelSpecs.channel_bounds.min_time = timeInterval.getStart() / 1000;
                    channelSpecs.channel_bounds.max_time = timeInterval.getEnd() / 1000;
                }
                channelSpecs.channel_bounds.min_value = .6;
                channelSpecs.channel_bounds.max_value = 1;

                infoResponse.channel_specs.put(channelName, channelSpecs);

                if (timeInterval != null) {
                    // update the min/max times in ChannelInfoResponse and in the All photos channel
                    infoResponse.min_time = Math.min(infoResponse.min_time, channelSpecs.channel_bounds.min_time);
                    infoResponse.max_time = Math.max(infoResponse.max_time, channelSpecs.channel_bounds.max_time);
                    allPhotosChannel.min_time = Math.min(allPhotosChannel.min_time, channelSpecs.channel_bounds.min_time);
                    allPhotosChannel.max_time = Math.max(allPhotosChannel.max_time, channelSpecs.channel_bounds.max_time);
                }
            }
        }
        return allPhotosSource;
    }

    private void populateResponseWithChannelMappings(Long guestId,
                                                     TrustedBuddy trustedBuddy,
                                                     SourcesResponse response,
                                                     List<ChannelMapping> channelMappings,
                                                     ChannelInfoResponse infoResponse) {
        for (ChannelMapping mapping : channelMappings){
            ApiKey api = guestService.getApiKey(mapping.getApiKeyId());
            // This is to prevent a rare condition when working, under development, on a branch that
            // doesn't yet support a connector that is supported on another branch and resulted
            // in data being populated in the database which is going to cause a crash here
            if (api==null||api.getConnector()==null)
                continue;
            // filter out not shared connectors
            if (trustedBuddy !=null&& buddiesService.getSharedConnector(api.getId(), AuthHelper.getGuestId())==null)
                continue;
            Source source;
            String deviceName;
            if (mapping.getInternalDeviceName()!=null&&!mapping.getInternalDeviceName().equals(mapping.getDeviceName())) {
                source = response.hasSource(mapping.getInternalDeviceName());
                deviceName = mapping.getInternalDeviceName();
            } else {
                source = response.hasSource(mapping.getDeviceName());
                deviceName = mapping.getDeviceName();
            }
            if (source == null){
                source = new Source();
                response.sources.add(source);
                source.name = Utils.sanitize(deviceName);
                source.channels = new ArrayList<Channel>();
                source.min_time = Double.MAX_VALUE;
                source.max_time = Double.MIN_VALUE;
            }
            Channel channel = new Channel();
            channel.name = Utils.sanitize(mapping.getChannelName());
            channel.type = mapping.getChannelType().name();
            channel.time_type = mapping.getTimeType().name();
            source.channels.add(channel);

            // Set builtin default style and style to a line by default
            channel.builtin_default_style = ChannelStyle.getDefaultChannelStyle(channel.name);
            channel.style = channel.builtin_default_style;

            // getDefaultStyle checks for user-generated overrides in the database.
            // If it returns non-null we set style to the user-generated value, otherwise we leave
            // it as the builtin default
            ChannelStyle userStyle = getDefaultStyle(guestId,api.getConnector().getDeviceNickname(),channel.name);
            if (userStyle != null) {
                channel.style = userStyle;
            } else {
                userStyle = getDefaultStyle(guestId,api.getConnector().getName(),channel.name);
                if (userStyle != null)
                    channel.style = userStyle;
            }

            setChannelBounds(mapping, channel, infoResponse);
            source.min_time = Math.min(source.min_time,channel.min_time);
            source.max_time = Math.max(source.max_time,channel.max_time);
        }
        long now = System.currentTimeMillis();
    }

    public SourceInfo getSourceInfoObject(final Long guestId, final String deviceName){
        try{
            if (guestId == null)
                throw new IllegalArgumentException();
            long then = System.currentTimeMillis();
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("info",new Object[]{"-r",guestId});
            String result = dataStoreExecutionResult.getResponse();
            long now = System.currentTimeMillis();
            System.out.println("datastore execution time = " + (now-then)); then = now;

            // TODO: check statusCode in DataStoreExecutionResult
            ChannelInfoResponse infoResponse = gson.fromJson(result,ChannelInfoResponse.class);

            final Map<String, TimeInterval> photoChannelTimeRanges = photoService.getPhotoChannelTimeRanges(guestId, null);
            if (!photoChannelTimeRanges.isEmpty()) {
                final double defaultTimeForNullTimeIntervals = System.currentTimeMillis() / 1000;

                for (final String channelName : photoChannelTimeRanges.keySet()) {
                    final ChannelSpecs channelSpecs = new ChannelSpecs();
                    final TimeInterval timeInterval = photoChannelTimeRanges.get(channelName);

                    // mark this channel as a photo channel so that the grapher can properly render it as a photo channel
                    channelSpecs.channelType = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;
                    final String[] connectorNameAndObjectTypeName = channelName.split("\\.");
                    if (connectorNameAndObjectTypeName.length > 1) {
                        channelSpecs.objectTypeName = connectorNameAndObjectTypeName[1];
                    }

                    channelSpecs.channel_bounds = new ChannelBounds();
                    if (timeInterval == null) {
                        channelSpecs.channel_bounds.min_time = defaultTimeForNullTimeIntervals;
                        channelSpecs.channel_bounds.max_time = defaultTimeForNullTimeIntervals;
                    }
                    else {
                        channelSpecs.channel_bounds.min_time = timeInterval.getStart() / 1000;
                        channelSpecs.channel_bounds.max_time = timeInterval.getEnd() / 1000;
                    }
                    channelSpecs.channel_bounds.min_value = .6;
                    channelSpecs.channel_bounds.max_value = 1;

                    infoResponse.channel_specs.put(channelName, channelSpecs);

                    if (timeInterval != null) {
                        // update the min/max times in ChannelInfoResponse and in the All photos channel
                        infoResponse.min_time = Math.min(infoResponse.min_time, channelSpecs.channel_bounds.min_time);
                        infoResponse.max_time = Math.max(infoResponse.max_time, channelSpecs.channel_bounds.max_time);
                    }
                }
            }

            SourceInfo response = new SourceInfo(infoResponse,deviceName);

            return response;
        }
        catch(Exception e){
            return new SourceInfo(null, null);
        }
    }

    public String getSourceInfo(final Long guestId, final String deviceName) {
        return gson.toJson(getSourceInfoObject(guestId,deviceName));
    }

    public void setDefaultStyle(final Long guestId, final String deviceName, final String channelName, final ChannelStyle style) {
        setDefaultStyle(guestId,deviceName,channelName, gson.toJson(style));
    }

    @Deprecated
    @Transactional(readOnly = false)
    public void deleteStyle(final Long guestId, final String deviceName) {
        try {
            JPAUtils.execute(em, "channelStyle.delete.byGuestAndDeviceName", guestId, deviceName);
        } catch(Exception e) {logger.warn("Couldn't delete Channel Style for connector "
                                          + deviceName
                                          + ", guest: " + guestId
                                          + "\n" + ExceptionUtils.getStackTrace(e));}
    }

    @Transactional(readOnly = false)
    public void setDefaultStyle(final Long guestId, final String deviceName, final String channelName, final String style) {
        try{
            if (guestId == null)
                throw new IllegalArgumentException();
            org.fluxtream.core.domain.ChannelStyle savedStyle = JPAUtils.findUnique(em, org.fluxtream.core.domain.ChannelStyle.class,
                                                          "channelStyle.byDeviceNameAndChannelName",
                                                          guestId, deviceName, channelName);

            if (savedStyle==null) {
                savedStyle = new org.fluxtream.core.domain.ChannelStyle();
                savedStyle.guestId = guestId;
                savedStyle.channelName = channelName;
                savedStyle.deviceName = deviceName;
                savedStyle.json = style;
                em.persist(savedStyle);
            } else {
                savedStyle.json = style;
                em.merge(savedStyle);
            }

            List<ApiKey> keys = guestService.getApiKeys(guestId,Connector.getConnector("fluxtream_capture"));
            long apiKeyId = -1;
            if (keys.size() > 0){
                apiKeyId = keys.get(0).getId();
            }
            dataUpdateService.logBodyTrackStyleUpdate(guestId,apiKeyId,null,deviceName,new String[]{channelName});


        }
        catch (Exception e){

        }
    }

    public String getDeviceName(long apiKeyId) {
        ChannelMapping channelMapping = JPAUtils.findUnique(em, ChannelMapping.class, "channelMapping.byApiKeyId", apiKeyId);
        if (channelMapping!=null)
            return channelMapping.getDeviceName();
        return null;
    }

    public String getInternalDeviceName(long apiKeyId) {
        ApiKey apiKey = guestService.getApiKey(apiKeyId);
        if (apiKey.getConnector().getName().equals("fluxtream_capture"))
            return getDeviceName(apiKeyId);
        ChannelMapping channelMapping = JPAUtils.findUnique(em, ChannelMapping.class, "channelMapping.byApiKeyId", apiKeyId);
        if (channelMapping!=null)
            return channelMapping.getInternalDeviceName();
        return null;
    }

    public ChannelMapping getChannelMapping(long guestId, String displayDeviceName, String displayChannelName){
        ChannelMapping channelMapping = JPAUtils.findUnique(em, ChannelMapping.class, "channelMapping.byDisplayName", guestId, displayDeviceName, displayChannelName);
        return channelMapping;
    }

    public List<ChannelMapping> getChannelMappings(long guestId, TrustedBuddy trustedBuddy){
        if (trustedBuddy ==null)
            return JPAUtils.find(em, ChannelMapping.class, "channelMapping.all",guestId);
        else {
            List<SharedChannel> sharedChannels = buddiesService.getSharedChannels(trustedBuddy.buddyId, trustedBuddy.guestId);
            List<ChannelMapping> channelMappings = new ArrayList<ChannelMapping>();
            for (SharedChannel sharedChannel : sharedChannels) {
                channelMappings.add(sharedChannel.channelMapping);
            }
            return channelMappings;
        }
    }

    public void deleteChannelMappings(ApiKey apiKey){
        Query query = em.createNamedQuery("channelMapping.delete");
        query.setParameter(1,apiKey.getGuestId());
        query.setParameter(2,apiKey.getId());
        query.executeUpdate();
    }

    public void setBuiltinDefaultStyle(final Long guestId, final String deviceName, final String channelName, final ChannelStyle style){
        setBuiltinDefaultStyle(guestId, deviceName, channelName, gson.toJson(style));
    }

    @Transactional(readOnly = false)
    public void setBuiltinDefaultStyle(final Long guestId, final String deviceName, final String channelName, final String style) {
        try{
            if (guestId == null)
                throw new IllegalArgumentException();
            org.fluxtream.core.domain.ChannelStyle savedStyle = JPAUtils.findUnique(em, org.fluxtream.core.domain.ChannelStyle.class, "channelStyle.byDeviceNameAndChannelName", guestId, deviceName, channelName);
            if (savedStyle==null) {
                savedStyle = new org.fluxtream.core.domain.ChannelStyle();
                savedStyle.guestId = guestId;
                savedStyle.channelName = channelName;
                savedStyle.deviceName = deviceName;
                savedStyle.json = style;
                em.persist(savedStyle);
            }
        }
        catch (Exception e){

        }
    }

    private ChannelStyle getDefaultStyle(long guestId, String deviceName, String channelName){
        org.fluxtream.core.domain.ChannelStyle savedStyle = JPAUtils.findUnique(em, org.fluxtream.core.domain.ChannelStyle.class,
                                                                           "channelStyle.byDeviceNameAndChannelName",
                                                                           guestId, deviceName, channelName);
        if(savedStyle == null)
            return null;
        return gson.fromJson(savedStyle.json,ChannelStyle.class);

    }

    @Transactional(readOnly = false)
    public String saveView(long guestId, String viewName, String viewJSON){
        GrapherView view = JPAUtils.findUnique(em, GrapherView.class,"grapherView.byName",guestId,viewName);
        if (view == null){
            view = new GrapherView();
            view.guestId = guestId;
            view.name = viewName;
            view.json = viewJSON;
            view.lastUsed = System.currentTimeMillis();
            em.persist(view);
        }
        else{
            view.json = viewJSON;
            em.merge(view);
        }
        AddViewResult result = new AddViewResult();
        result.saved_view_id = view.getId();
        result.populateViews(em, gson, guestId);
        return gson.toJson(result);
    }

    public String listViews(long guestId){
        ViewsList list = new ViewsList();
        list.populateViews(em, gson, guestId);
        return gson.toJson(list);
    }

    @Transactional(readOnly = false)
    public String getView(Long guestId, long viewId){
        GrapherView view = JPAUtils.findUnique(em, GrapherView.class,"grapherView.byId",guestId,viewId);
        if (view != null){
            view.lastUsed = System.currentTimeMillis();
            em.merge(view);
        }
        return view == null ? "{\"error\",\"No matching view found for user " + guestId + "\"}" : view.json;
    }

    @Transactional(readOnly = false)
    public void deleteView(Long guestId, long viewId){
        GrapherView view = JPAUtils.findUnique(em, GrapherView.class,"grapherView.byId",guestId,viewId);
        em.remove(view);
    }

    public String getAllTagsForUser(final Long guestId) {
        final List<Tag> tagList = JPAUtils.find(em, Tag.class, "tags.all", guestId);

        final TagsJson tagsJson = new TagsJson();
        if ((tagList != null) && (!tagList.isEmpty())) {
            for (final Tag tag : tagList) {
                if (tag != null && tag.name != null && tag.name.length() > 0) {
                    tagsJson.tags.add(tag.name);
                }
            }
        }

        return gson.toJson(tagsJson);
    }

    private static class TagsJson {
        private final SortedSet<String> tags = new TreeSet<String>();
    }

    @ApiModel
    public static class ViewsList{
        @ApiModelProperty
        public LinkedList<ViewStub> views = new LinkedList<ViewStub>();

        void populateViews(EntityManager em, Gson gson, long guestId){
            List<GrapherView> viewList = JPAUtils.find(em, GrapherView.class,"grapherView",guestId);
            for (GrapherView view : viewList){
                views.add(0,new ViewStub(view,gson));
            }
        }
    }

    public static class AddViewResult extends ViewsList{
        public long saved_view_id;
    }

    @ApiModel
    public static class ViewStub{
        @ApiModelProperty
        public long id;
        @ApiModelProperty
        public long last_used;
        @ApiModelProperty
        public String name;
        @ApiModelProperty
        public AxisRange time_range;

        public ViewStub(GrapherView view,Gson gson){
            id = view.getId();
            last_used = view.lastUsed;
            name = view.name;
            ViewJSON json = gson.fromJson(view.json,ViewJSON.class);
            time_range = new AxisRange();
            time_range.min = json.v2.x_axis.min * 1000;
            time_range.max = json.v2.x_axis.max * 1000;
        }

    }

    public static class ViewJSON{
        public String name;
        public ViewData v2;
    }

    public static class ViewData{
        public AxisRange x_axis;
        public boolean show_add_pane;
        public ArrayList<ViewChannelData> y_axes;
    }

    public static class AxisRange{
        public double min;
        public double max;
    }

    public static class GetTileResponse{
        public Object[][] data;
        public String[] fields;
        public int level;
        public long offset;
        public int sample_width;
        public String type = "value";

        public static GetTileResponse getEmptyTile(int level, long offset){
            GetTileResponse tileResponse = new GetTileResponse();
            tileResponse.data = new Object[0][];
            tileResponse.level = level;
            tileResponse.offset = offset;
            tileResponse.fields = new String[]{"time", "mean", "stddev", "count"};
            return tileResponse;
        }
    }

    private static class ChannelInfoResponse {
        Map<String,ChannelSpecs> channel_specs;
        double max_time;
        double min_time;
    }


    private static class ChannelSpecs{
        String channelType;
        String objectTypeName;
        String time_type;
        ChannelBounds channel_bounds;

        public ChannelSpecs(){
            // time_type defaults to gmt.  It can be overridden to "local" for channels that only know local time
            time_type = "gmt";
        }
    }

    private static class ChannelBounds{
        double max_time;
        double max_value;
        double min_time;
        double min_value;
    }

    class ChannelBoundsDeserializer implements JsonDeserializer<ChannelBounds>{
        // Create a custom deserializer for ChannelBounds to deal with the possibility
        // of the values being interpreted as +/- Infinity and causing json creation errors
        // later on.  The min/max time fields are required.  The min/max value fields are
        // optional and default to 0.
        @Override
        public ChannelBounds deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {
            ChannelBounds cb=new ChannelBounds();

            JsonObject jo = (JsonObject)json;
            cb.max_time=Math.max(Math.min(jo.get("max_time").getAsDouble(), Double.MAX_VALUE),-Double.MAX_VALUE);
            cb.min_time=Math.max(Math.min(jo.get("min_time").getAsDouble(), Double.MAX_VALUE), -Double.MAX_VALUE);

            try {
                cb.max_value=Math.max(Math.min(jo.get("max_value").getAsDouble(), Double.MAX_VALUE),-Double.MAX_VALUE);
                cb.min_value=Math.max(Math.min(jo.get("min_value").getAsDouble(), Double.MAX_VALUE), -Double.MAX_VALUE);
            } catch(Throwable e) {
                cb.min_value=cb.max_value=0;
            }

            return cb;
        }
    }

    @ApiModel
    public class SourcesResponse {
        @ApiModelProperty
        public List<Source> sources = new ArrayList<Source>();

        public SourcesResponse() {}

        public SourcesResponse(ChannelInfoResponse infoResponse, Long guestId, TrustedBuddy trustedBuddy){
            sources = new ArrayList<Source>();
            if (infoResponse == null)
                return;

            for (Map.Entry<String,ChannelSpecs> entry : infoResponse.channel_specs.entrySet()){
                String fullName = entry.getKey();
                ChannelSpecs specs = entry.getValue();
                String[] split = fullName.split("\\.");
                // device.objectTypeName._comment should not generate an entry
                if (split.length>2)
                    continue;
                String deviceName = split[0];
                String objectTypeName = split[1];
                Source source = null;
                if (trustedBuddy ==null || trustedBuddy.hasAccessToDevice(deviceName, env)) {
                    for (Source src : sources)
                        if (src.name.equals(deviceName)){
                            source = src;
                            break;
                        }
                    if (source == null){
                        source = new Source();
                        source.name = deviceName;
                        source.channels = new ArrayList<Channel>();
                        sources.add(source);
                    }

                    Channel newChannel = new Channel(objectTypeName,specs);

                    // Setup style settings.  The Channel constructor sets builtin_default_style
                    // and style to default line settings.  getDefaultStyle checks for user-generated overrides
                    // in the database.  If it returns non-null we set style to the user-generated value.
                    ChannelStyle userStyle = getDefaultStyle(guestId,source.name,newChannel.name);
                    if (userStyle != null)
                        newChannel.style = userStyle;

                    // Temporary hack: Until generic support is available for time_type, special case
                    // devices named 'Zeo', 'Fitbit', or 'Flickr' to use time_type="local"
                    if(source.name.equals("Zeo") || source.name.equals("Fitbit")  || source.name.equals("Flickr")) {
                        newChannel.time_type="local";
                    }

                    // Add channel to source's channel list
                    source.channels.add(newChannel);
                }
            }

        }

        public Source hasSource(String deviceName){
            for (Source s : sources){
                if (s.name.equals(deviceName))
                    return s;
            }
            return null;
        }

        public void deleteSource(String deviceName){
            for (Iterator<Source> i = sources.iterator(); i.hasNext();){
                Source s = i.next();
                if (s.name.equals(deviceName))
                    i.remove();
            }

        }
    }

    @ApiModel
    public static class SourceInfo{

        @ApiModelProperty
        public Source info;

        public SourceInfo(ChannelInfoResponse infoResponse, String deviceName){
            info = new Source();
            info.name = deviceName;
            info.channels = new ArrayList<Channel>();
            info.max_time = System.currentTimeMillis() / 1000.0;
            info.min_time = info.max_time - 1;
            if (infoResponse == null)
                return;

            for (Map.Entry<String,ChannelSpecs> entry : infoResponse.channel_specs.entrySet()){
                String fullName = entry.getKey();
                ChannelSpecs specs = entry.getValue();
                String[] split = fullName.split("\\.");
                // device.objectTypeName._comment should not generate an entry
                if (split.length>2)
                    continue;
                String devName = split[0];
                String objectTypeName = split[1];
                Source source = null;
                if (devName.equals(deviceName)){
                    Channel channel = new Channel(objectTypeName,specs);
                    info.channels.add(channel);
                    if (channel.min_time < info.min_time)
                        info.min_time = channel.min_time;
                    if (channel.max_time > info.max_time)
                        info.max_time = channel.max_time;
                }
            }
        }

    }

    @ApiModel
    public static class Source{
        @ApiModelProperty
        public String name;
        @ApiModelProperty
        public List<Channel> channels;
        @ApiModelProperty
        public Double min_time = 0.0;
        @ApiModelProperty
        public Double max_time = 0.0;
    }

    @ApiModel
    public static class Channel{
        @ApiModelProperty
        public String type;
        @ApiModelProperty
        public ChannelStyle builtin_default_style;
        @ApiModelProperty
        public ChannelStyle style;
        @ApiModelProperty
        public double max;
        @ApiModelProperty
        public double min;
        @ApiModelProperty
        public Double min_time;
        @ApiModelProperty
        public Double max_time;
        @ApiModelProperty
        public String name;
        @ApiModelProperty
        public String objectTypeName;
        @ApiModelProperty
        public String time_type;

        public Channel(){
            // time_type defaults to gmt.  It can be overridden to "local" for channels that only know local time
            time_type = "gmt";
        }
        public Channel(String name, ChannelSpecs specs){
            this.name = name;
            max = specs.channel_bounds.max_value;
            min = specs.channel_bounds.min_value;
            min_time = specs.channel_bounds.min_time;
            max_time = specs.channel_bounds.max_time;
            if (specs.channelType != null) {
                this.name = PhotoService.DEFAULT_PHOTOS_CHANNEL_NAME;   // photo channels are always named the same
                type = specs.channelType;
            }
            if (specs.objectTypeName != null) {
                objectTypeName = specs.objectTypeName;
            }
            style = builtin_default_style = ChannelStyle.getDefaultChannelStyle(name);
            // time_type defaults to gmt.  It can be overridden to "local" for channels that only know local time
            time_type = specs.time_type;
        }
    }

    public static class ViewChannelData extends Channel{
        public Integer channel_height;
        public String channel_name;
        public String device_name;
    }


    @ApiModel
    public static class ChannelStyle{
        @ApiModelProperty
        public HighlightStyling highlight;
        @ApiModelProperty
        public CommentStyling comments;
        @ApiModelProperty
        public List<Style> styles;
        @ApiModelProperty
        public MainTimespanStyle timespanStyles;

        public static ChannelStyle getDefaultChannelStyle(String name){
            ChannelStyle style = new ChannelStyle();
            style.styles = new ArrayList<Style>();
             if (name.equals("Sleep_Graph")){
                 Style subStyle = new Style();
                 subStyle.type = "zeo";
                 subStyle.show = true;
                 style.styles.add(subStyle);
             }
            else{
                 Style subStyle = new Style();
                 subStyle.type = "line";
                 subStyle.lineWidth = 1;
                 subStyle.show = true;
                 style.styles.add(subStyle);
             }
            return style;
        }

    }

    public static class TimespanStyle{
        public Integer borderWidth;
        public String borderColor;
        public String fillColor;
        public Double top;
        public Double bottom;
        public String iconURL;
    }

    public static class MainTimespanStyle{
        public TimespanStyle defaultStyle;
        public Map<String, TimespanStyle> values;
    }

    public static class CommentStyling{
        public Boolean show;
        public Integer verticalMargin;
        public ArrayList<Style> styles;
    }

    public static class HighlightStyling{
        public Integer lineWidth;
        public ArrayList<Style> styles;
    }

    public static class Style{
        public String type;
        public Integer lineWidth;
        public String color;
        public String fillColor;
        public Integer marginWidth;
        public String numberFormat;
        public Integer verticalOffset;
        public Integer radius;
        public Boolean fill;
        public Boolean show;
    }

    public static final class UploadResponseChannelSpecs{
        ChannelBounds imported_bounds;
        ChannelBounds channel_bounds;


    }

    public static final class UploadResponse{
        Map<String,UploadResponseChannelSpecs> channel_specs;
        int failed_records;
        int successful_records;
        Double max_time; //seconds
        Double min_time; //seconds

    }

    public static final class ParsedBodyTrackUploadResult implements BodyTrackUploadResult {
        private int statusCode;
        private String responseText;
        private UploadResponse parsedResponse;
        private String deviceName;



        private ParsedBodyTrackUploadResult(BodyTrackUploadResult result, String deviceName,Gson gson){
            this.statusCode = result.getStatusCode();
            this.responseText = result.getResponse();
            this.deviceName = deviceName;
            if (result.isSuccess())
                this.parsedResponse = gson.fromJson(responseText,UploadResponse.class);
            else {
                this.parsedResponse = new UploadResponse();
                logger.warn("Couldn't upload to bodytrack, (response text is \"" + responseText + "\", statusCode: " + statusCode + ", deviceName: " + deviceName + ")");
            }
        }

        public UploadResponse getParsedResponse(){
            return parsedResponse;
        }

        public String getDeviceName(){
            return deviceName;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getResponse() {
            return responseText;
        }

        @Override
        public boolean isSuccess() {
            return statusCode == 0;
        }
    }

    public static final class DataStoreExecutionResult implements BodyTrackUploadResult {
        private final int statusCode;
        private final String response;

        private DataStoreExecutionResult(final int statusCode, final String response) {
            this.statusCode = statusCode;
            this.response = response;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getResponse() {
            return response;
        }

        @Override
        public boolean isSuccess() {
            return statusCode == 0;
        }
    }
}
