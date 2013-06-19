package com.fluxtream.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.Configuration;
import com.fluxtream.TimeInterval;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.domain.CoachingBuddy;
import com.fluxtream.domain.GrapherView;
import com.fluxtream.domain.Tag;
import com.fluxtream.services.PhotoService;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @PersistenceContext
    EntityManager em;

    static final boolean verboseOutput = false;
    static final boolean showOutput = false;

    @Autowired
    Configuration env;

    @Autowired
    PhotoService photoService;

    // Create a Gson parser which handles ChannelBounds specially to avoid problems with +/- infinity
    Gson gson = new GsonBuilder().registerTypeAdapter(ChannelBounds.class, new ChannelBoundsDeserializer()).create();
    static FlxLogger logger = FlxLogger.getLogger(BodyTrackHelper.class);

    private DataStoreExecutionResult executeDataStore(String commandName, Object[] parameters){
        try{
            Runtime rt = Runtime.getRuntime();
            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/" + commandName + " " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location");
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
            String result = "";


            while((line=input.readLine()) != null) { //output all console output from the execution
                if (showOutput)
                    System.out.println("BTDataStore: " + line);
                result += line;
            }
            int exitValue = pr.waitFor();
            if (showOutput)
                System.out.println("BTDataStore: exited with code " + exitValue);
            return new DataStoreExecutionResult(exitValue, result);
        }
        catch (Exception e){
            if (showOutput)
                System.out.println("BTDataStore: datastore execution failed!");
            throw new RuntimeException("Datastore execution failed");
        }
    }

    public BodyTrackUploadResult uploadToBodyTrack(final Long uid,
                                  final String deviceName,
                                  final Collection<String> channelNames,
                                  final List<List<Object>> data) {
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            final File tempFile = File.createTempFile("input",".json");

            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data", data);
            tempFileMapping.put("channel_names",channelNames);

            FileOutputStream fos = new FileOutputStream(tempFile);
            final String bodyTrackJSONData = gson.toJson(tempFileMapping);
            fos.write(bodyTrackJSONData.getBytes());
            fos.close();

            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("import", new Object[]{uid, deviceName, tempFile.getAbsolutePath()});
            tempFile.delete();
            return dataStoreExecutionResult;
        } catch (Exception e) {
            System.out.println("Could not persist to datastore");
            System.out.println(Utils.stackTrace(e));
            throw new RuntimeException("Could not persist to datastore");
        }
    }

    public BodyTrackUploadResult uploadJsonToBodyTrack(final Long uid,
                                      final String deviceName,
                                      final String json) {
         try{
             if (uid == null)
                 throw new IllegalArgumentException();
             final File tempFile = File.createTempFile("input",".json");

             FileOutputStream fos = new FileOutputStream(tempFile);
             fos.write(json.getBytes());
             fos.close();

             final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("import",new Object[]{uid,deviceName,tempFile.getAbsolutePath()});
             tempFile.delete();
             return dataStoreExecutionResult;
         } catch (Exception e) {
             System.out.println("Could not persist to datastore");
             System.out.println(Utils.stackTrace(e));
             throw new RuntimeException("Could not persist to datastore");
         }
     }

    public String fetchTile(Long uid, String deviceNickname, String channelName, int level, long offset){
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("gettile", new Object[]{uid, deviceNickname + "." + channelName, level, offset});
            String result = dataStoreExecutionResult.getResponse();

            // TODO: check statusCode in DataStoreExecutionResult
            GetTileResponse tileResponse = gson.fromJson(result,GetTileResponse.class);

            if (tileResponse.data == null){
                tileResponse = GetTileResponse.getEmptyTile(level,offset);
            }//TODO:several fields are missing still and should be implemented

            return gson.toJson(tileResponse);
        }
        catch(Exception e){
            return gson.toJson(GetTileResponse.getEmptyTile(level,offset));
        }
    }

    public String listSources(Long uid, CoachingBuddy coachee){
        SourcesResponse response = null;
        try{
            if (uid == null) {
                throw new IllegalArgumentException();
            }
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("info",new Object[]{"-r",uid});
            String result = dataStoreExecutionResult.getResponse();

            // TODO: check statusCode in DataStoreExecutionResult
            ChannelInfoResponse infoResponse = gson.fromJson(result,ChannelInfoResponse.class);

            // Iterate over the various (photo) connectors (if any), manually inserting each into the ChannelSpecs
            final Map<String, TimeInterval> photoChannelTimeRanges = photoService.getPhotoChannelTimeRanges(uid, coachee);

            // create the 'All' photos block
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
                        channelSpecs.channel_bounds.min_time = timeInterval.start / 1000;
                        channelSpecs.channel_bounds.max_time = timeInterval.end / 1000;
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

            // create the respone
            response = new SourcesResponse(infoResponse, coachee);

            // add the All photos block to the response
            if (!photoChannelTimeRanges.isEmpty()) {
                response.sources.add(allPhotosSource);
            }

            // set default styles if necessary
            for (Source source : response.sources){
                for (Channel channel : source.channels){
                    ChannelStyle userStyle = getDefaultStyle(uid,source.name,channel.name);
                    if (userStyle != null)
                        channel.style = userStyle;
                    // Temporary hack: Until generic support is available for time_type, special case
                    // devices named 'Zeo', 'Fitbit', or 'Flickr' to use time_type="local"
                    if(source.name.equals("Zeo") || source.name.equals("Fitbit")  || source.name.equals("Flickr")) {
                        
                        channel.time_type="local";
                    }
                }
            }

            return gson.toJson(response);
        }
        catch(Exception e){
            StringBuilder sb = new StringBuilder("module=bodytrackHelper component=listSources action=listSources")
                    .append(" guestId=")
                    .append(uid)
                    .append(" message=").append(e.getMessage());

            if(response!=null) {
                // In case the exception was caused by speical floating point values such as
                // Infinity, create a gson builder that will potentially let us debug even though
                // the javascript would choke on the result if we returned it
                Gson errorGson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
                sb.append(" response=").append(errorGson.toJson(response));
            }

            logger.error(sb.toString());

            return gson.toJson(new SourcesResponse(null, coachee));
        }
    }

    public SourceInfo getSourceInfoObject(final Long uid, final String deviceName){
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            final DataStoreExecutionResult dataStoreExecutionResult = executeDataStore("info",new Object[]{"-r",uid});
            String result = dataStoreExecutionResult.getResponse();

            // TODO: check statusCode in DataStoreExecutionResult
            ChannelInfoResponse infoResponse = gson.fromJson(result,ChannelInfoResponse.class);

            final Map<String, TimeInterval> photoChannelTimeRanges = photoService.getPhotoChannelTimeRanges(uid, null);
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
                        channelSpecs.channel_bounds.min_time = timeInterval.start / 1000;
                        channelSpecs.channel_bounds.max_time = timeInterval.end / 1000;
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

    public String getSourceInfo(final Long uid, final String deviceName) {
        return gson.toJson(getSourceInfoObject(uid,deviceName));
    }

    public void setDefaultStyle(final Long uid, final String deviceName, final String channelName, final ChannelStyle style) {
        setDefaultStyle(uid,deviceName,channelName, gson.toJson(style));
    }

    @Transactional(readOnly = false)
    public void setDefaultStyle(final Long uid, final String deviceName, final String channelName, final String style) {
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            com.fluxtream.domain.ChannelStyle savedStyle = JPAUtils.findUnique(em, com.fluxtream.domain.ChannelStyle.class,
                                                          "channelStyle.byDeviceNameAndChannelName",
                                                          uid, deviceName, channelName);
            if (savedStyle==null) {
                savedStyle = new com.fluxtream.domain.ChannelStyle();
                savedStyle.guestId = uid;
                savedStyle.channelName = channelName;
                savedStyle.deviceName = deviceName;
                savedStyle.json = style;
                em.persist(savedStyle);
            } else {
                savedStyle.json = style;
                em.merge(savedStyle);
            }
        }
        catch (Exception e){

        }
    }

    @Transactional(readOnly = false)
    public void setBuiltinDefaultStyle(final Long uid, final String deviceName, final String channelName, final String style) {
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            com.fluxtream.domain.ChannelStyle savedStyle = JPAUtils.findUnique(em, com.fluxtream.domain.ChannelStyle.class, "channelStyle.byDeviceNameAndChannelName", uid, deviceName, channelName);
            if (savedStyle==null) {
                savedStyle = new com.fluxtream.domain.ChannelStyle();
                savedStyle.guestId = uid;
                savedStyle.channelName = channelName;
                savedStyle.deviceName = deviceName;
                savedStyle.json = style;
                em.persist(savedStyle);
            }
        }
        catch (Exception e){

        }
    }

    private ChannelStyle getDefaultStyle(long uid, String deviceName, String channelName){
        com.fluxtream.domain.ChannelStyle savedStyle = JPAUtils.findUnique(em, com.fluxtream.domain.ChannelStyle.class,
                                                                           "channelStyle.byDeviceNameAndChannelName",
                                                                           uid, deviceName, channelName);
        if(savedStyle == null)
            return null;
        return gson.fromJson(savedStyle.json,ChannelStyle.class);

    }

    @Transactional(readOnly = false)
    public String saveView(long uid, String viewName, String viewJSON){
        GrapherView view = JPAUtils.findUnique(em, GrapherView.class,"grapherView.byName",uid,viewName);
        if (view == null){
            view = new GrapherView();
            view.guestId = uid;
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
        result.populateViews(em, gson, uid);
        return gson.toJson(result);
    }

    public String listViews(long uid){
        ViewsList list = new ViewsList();
        list.populateViews(em, gson, uid);
        return gson.toJson(list);
    }

    @Transactional(readOnly = false)
    public String getView(Long uid, long viewId){
        GrapherView view = JPAUtils.findUnique(em, GrapherView.class,"grapherView.byId",uid,viewId);
        if (view != null){
            view.lastUsed = System.currentTimeMillis();
            em.merge(view);
        }
        return view == null ? "{\"error\",\"No matching view found for user " + uid + "\"}" : view.json;
    }

    @Transactional(readOnly = false)
    public void deleteView(Long uid, long viewId){
        GrapherView view = JPAUtils.findUnique(em, GrapherView.class,"grapherView.byId",uid,viewId);
        em.remove(view);
    }

    public String getAllTagsForUser(final Long uid) {
        final List<Tag> tagList = JPAUtils.find(em, Tag.class, "tags.all", uid);

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

    private static class ViewsList{
        LinkedList<ViewStub> views = new LinkedList<ViewStub>();

        void populateViews(EntityManager em, Gson gson, long uid){
            List<GrapherView> viewList = JPAUtils.find(em, GrapherView.class,"grapherView",uid);
            for (GrapherView view : viewList){
                views.add(0,new ViewStub(view,gson));
            }
        }
    }

    public static class AddViewResult extends ViewsList{
        long saved_view_id;
    }

    private static class ViewStub{
        long id;
        long last_used;
        String name;
        AxisRange time_range;

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

    private static class ViewJSON{
        String name;
        ViewData v2;
    }

    private static class ViewData{
        AxisRange x_axis;
        boolean show_add_pane;
        ArrayList<ViewChannelData> y_axes;
    }

    private static class AxisRange{
        double min;
        double max;
    }

    private static class GetTileResponse{
        Object[][] data;
        String[] fields;
        int level;
        long offset;
        int sample_width;

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

    private class SourcesResponse {
        ArrayList<Source> sources;

        public SourcesResponse(ChannelInfoResponse infoResponse, CoachingBuddy coachee){
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
                if (coachee==null || coachee.hasAccessToDevice(deviceName, env)) {
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
                    source.channels.add(new Channel(objectTypeName,specs));
                }
            }

        }
    }

    public static class SourceInfo{
        Source info;

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

    public static class Source{
        String name;
        ArrayList<Channel> channels;
        Double min_time;
        Double max_time;
    }

    public static class Channel{
        String type;
        ChannelStyle builtin_default_style;
        ChannelStyle style;
        double max;
        double min;
        Double min_time;
        Double max_time;
        String name;
        String objectTypeName;
        String time_type;

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
        Integer channel_height;
        String channel_name;
        String device_name;
    }


    public static class ChannelStyle{
        HighlightStyling highlight;
        CommentStyling comments;
        ArrayList<Style> styles;

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

    public static class CommentStyling{
        Boolean show;
        Integer verticalMargin;
        ArrayList<Style> styles;
    }

    public static class HighlightStyling{
        Integer lineWidth;
        ArrayList<Style> styles;
    }

    public static class Style{
        String type;
        Integer lineWidth;
        String color;
        String fillColor;
        Integer marginWidth;
        String numberFormat;
        Integer verticalOffset;
        Integer radius;
        Boolean fill;
        Boolean show;
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
