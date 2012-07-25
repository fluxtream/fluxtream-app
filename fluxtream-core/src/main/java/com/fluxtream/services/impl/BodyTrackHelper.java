package com.fluxtream.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.fluxtream.Configuration;
import com.fluxtream.domain.GrapherView;
import com.fluxtream.utils.JPAUtils;
import com.fluxtream.utils.Utils;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
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

    @PersistenceContext
    EntityManager em;

    static Logger LOG = Logger.getLogger(BodyTrackHelper.class);

    static final boolean verboseOutput = false;

    @Autowired
    Configuration env;

    Gson gson = new Gson();

    private String executeDataStore(String commandName, Object[] parameters){
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
            System.out.println("BTDataStore: running with command: " + launchCommand);

            //create process for operation
            final Process pr = rt.exec(launchCommand);

            new Thread(){//outputs the errorstream
                public void run(){
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line=null;
                    try{
                        if (verboseOutput){
                            while((line=error.readLine()) != null) { //output all console output from the execution
                                System.out.println("BTDataStore-error: " + line);
                            }
                        }
                        else
                            while (error.readLine() != null);
                    } catch(Exception ignored){}
                }

            }.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line;
            String result = "";

            while((line=input.readLine()) != null) { //output all console output from the execution
                System.out.println("BTDataStore: " + line);
                result += line;
            }

            int exitValue = pr.waitFor();
            System.out.println("BTDataStore: exited with code " + exitValue);
            return result;
        }
        catch (Exception e){
            System.out.println("BTDataStore: datastore execution failed!");
            return null;
        }
    }

    public void uploadToBodyTrack(final Long uid,
                                  final String deviceName,
                                  final Collection<String> channelNames,
                                  final List<List<Object>> data) {
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            final File tempFile = File.createTempFile("input",".json");

            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data",data);
            tempFileMapping.put("channel_names",channelNames);

            FileOutputStream fos = new FileOutputStream(tempFile);
            final String bodyTrackJSONData = gson.toJson(tempFileMapping);
            fos.write(bodyTrackJSONData.getBytes());
            fos.close();

            executeDataStore("import",new Object[]{uid,deviceName,tempFile.getAbsolutePath()});
            tempFile.delete();
        } catch (Exception e) {
            System.out.println("Could not persist to datastore");
            System.out.println(Utils.stackTrace(e));
        }
    }

    public String fetchTile(Long uid, String deviceNickname, String channelName, int level, int offset){
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            String result = executeDataStore("gettile",new Object[]{uid,deviceNickname + "." + channelName,level,offset});

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

    public String listSources(Long uid){
        try{
            if (uid == null)
                throw new IllegalArgumentException();
            String result = executeDataStore("info",new Object[]{"-r",uid});

            channelInfoResponse infoResponse = gson.fromJson(result,channelInfoResponse.class);

            SourcesResponse response = new SourcesResponse(infoResponse);

            for (Source source : response.sources){
                for (Channel channel : source.channels){
                    ChannelStyle userStyle = getDefaultStyle(uid,source.name,channel.name);
                    if (userStyle != null)
                        channel.style = userStyle;
                }
            }

            return gson.toJson(response);
        }
        catch(Exception e){
            return gson.toJson(new SourcesResponse(null));
        }
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
        result.populateViews(em, uid);
        return gson.toJson(result);
    }

    public String listViews(long uid){
        ViewsList list = new ViewsList();
        list.populateViews(em,uid);
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

    private static class ViewsList{
        ArrayList<ViewStub> views = new ArrayList<ViewStub>();

        void populateViews(EntityManager em, long uid){
            List<GrapherView> viewList = JPAUtils.find(em, GrapherView.class,"grapherView",uid);
            for (GrapherView view : viewList){
                views.add(new ViewStub(view));
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

        public ViewStub(GrapherView view){
            id = view.getId();
            last_used = view.lastUsed;
            name = view.name;
        }

    }

    private static class GetTileResponse{
        Object[][] data;
        String[] fields;
        int level;
        int offset;
        int sample_width;

        public static GetTileResponse getEmptyTile(int level, int offset){
            GetTileResponse tileResponse = new GetTileResponse();
            tileResponse.data = new Object[0][];
            tileResponse.level = level;
            tileResponse.offset = offset;
            tileResponse.fields = new String[]{"time", "mean", "stddev", "count"};
            return tileResponse;
        }
    }

    private static class channelInfoResponse{
        Map<String,ChannelSpecs> channel_specs;
        double max_time;
        double min_time;
    }


    private static class ChannelSpecs{
        ChannelBounds channel_bounds;
    }

    private static class ChannelBounds{
        double max_time;
        double max_value;
        double min_time;
        double min_value;
    }

    private static class SourcesResponse{
        ArrayList<Source> sources;

        public SourcesResponse(channelInfoResponse infoResponse){
            sources = new ArrayList<Source>();
            if (infoResponse == null)
                return;

            for (Map.Entry<String,ChannelSpecs> entry : infoResponse.channel_specs.entrySet()){
                String fullName = entry.getKey();
                ChannelSpecs specs = entry.getValue();
                String[] split = fullName.split("\\.");
                String deviceName = split[0];
                String channelName = split[1];
                Source source = null;
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
                source.channels.add(new Channel(channelName,specs));
            }

        }
    }

    private static class Source{
        String name;
        ArrayList<Channel> channels;
    }

    private static class Channel{
        ChannelStyle builtin_default_style;
        ChannelStyle style;
        double max;
        double min;
        double min_time;
        double max_time;
        String name;

        public Channel(String name, ChannelSpecs specs){
            this.name = name;
            max = specs.channel_bounds.max_value;
            min = specs.channel_bounds.min_value;
            min_time = specs.channel_bounds.min_time;
            max_time = specs.channel_bounds.max_time;
            style = builtin_default_style = ChannelStyle.getDefaultChannelStyle(name);
        }
    }

    private static class ChannelStyle{
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

    private static class CommentStyling{
        Boolean show;
        Integer verticalMargin;
        ArrayList<Style> styles;
    }

    private static class HighlightStyling{
        Integer lineWidth;
        ArrayList<Style> styles;
    }

    private static class Style{
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

}
