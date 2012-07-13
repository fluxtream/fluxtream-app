package com.fluxtream.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fluxtream.Configuration;
import com.fluxtream.utils.HttpUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class BodyTrackHelper {

    static Logger LOG = Logger.getLogger(BodyTrackHelper.class);

    @Autowired
    Configuration env;

    Gson gson = new Gson();

    public void uploadToBodyTrack(long guestId, String deviceName, Collection<String> channelNames, List<List<Object>> data, Map<String,Object> channelSpecs) {
        try {
            /*Map<String, String> params = new HashMap<String,String>();
            params.put("dev_nickname", deviceName);
            params.put("channel_names", gson.toJson(channelNames));
            params.put("data", gson.toJson(data));

            String result = HttpUtils.fetch("http://" + host + "/users/"
                                            + user_id + "/upload", params, env);
            if (result.toLowerCase().startsWith("awesome")) {
                LOG.info("Data successfully uploaded to BodyTrack: guestId: "
                         + user_id);
            } else {
                LOG.warn("Could not upload data to BodyTrack data store: "
                         + result);
            }*/

            File tempFile = File.createTempFile("input",".json");
            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data",data);
            tempFileMapping.put("channel_names",channelNames);
            tempFileMapping.put("channel_specs",gson.toJson(channelSpecs));
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(gson.toJson(tempFileMapping).getBytes());
            fos.close();
            Runtime rt = Runtime.getRuntime();


            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/import " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location") + " " + guestId + " " +
                                   deviceName + " \"" + tempFile.getAbsolutePath() + "\"";
            System.out.println("BTDataStore: running with command: " + launchCommand);

            //create process for operation
            final Process pr = rt.exec(launchCommand);


            new Thread(){//outputs the errorstream
                public void run(){
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line=null;
                    try{
                    while((line=error.readLine()) != null) { //output all console output from the execution
                        System.out.println("BTDataStore: " + line);
                    }
                    } catch(Exception e){}
                }

            }.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line=null;

            while((line=input.readLine()) != null) { //output all console output from the execution
                System.out.println("BTDataStore: " + line);
            }

            //get the exit value
            int exitValue = pr.waitFor();
            System.out.println("BTDataStore: exited with code " + exitValue);
            tempFile.delete();
        } catch (Exception e) {
            LOG.warn("Could not upload data to BodyTrack data store: "
                     + e.getMessage());
        }
    }

    //TODO: update code to not use the alternate method directly
    public void uploadToBodyTrack(long guestId, final Map<String, String> params) {
        uploadToBodyTrack(guestId,params.get("dev_nickname"),gson.fromJson(params.get("channel_names"),List.class),
                        gson.fromJson(params.get("data"),List.class),gson.fromJson(params.get("channel_sepcs"),Map.class));

        /*try {
            String result = HttpUtils.fetch("http://" + host + "/users/"
                                            + user_id + "/upload", params, env);
            if (result.toLowerCase().startsWith("awesome")) {
                LOG.info("Data successfully uploaded to BodyTrack: guestId: "
                         + user_id);
            } else {
                LOG.warn("Could not upload data to BodyTrack data store: "
                         + result);
            }
        } catch (Exception e) {
            LOG.warn("Could not upload data to BodyTrack data store: "
                     + e.getMessage());
        }*/
    }

    public String fetchTile(String uid, String deviceNickname, String channelName, int level, int offset){
        try{
            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/gettile " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location") + " " + uid + " " +
                                   deviceNickname + "." + channelName + " " + level + " " + offset;
            System.out.println("BTDataStore: running with command: " + launchCommand);

            Runtime rt = Runtime.getRuntime();

            //create process for operation
            final Process pr = rt.exec(launchCommand);


            new Thread(){//outputs the errorstream
                public void run(){
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line=null;
                    try{
                        while((line=error.readLine()) != null) { //output all console output from the execution
                            System.out.println("BTDataStore-error: " + line);
                        }
                    } catch(Exception e){}
                }

            }.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line=null;

            String result = "";

            while((line=input.readLine()) != null) { //output all console output from the execution
                System.out.println("BTDataStore: " + line);
                result += line;
            }

            GetTileResponse tileResponse = gson.fromJson(result,GetTileResponse.class);

            Map<String,Object> resultMapping = new HashMap<String,Object>();

            if (tileResponse.data == null){
                tileResponse.data = new Object[0][];
                tileResponse.level = level;
                tileResponse.offset = offset;
                tileResponse.fields = new String[]{"time", "mean", "stddev", "count"};
            }//TODO:several fields are missing still and should be implemented
            tileResponse.putIntoMap(resultMapping);



            //get the exit value
            int exitValue = pr.waitFor();
            System.out.println("BTDataStore: exited with code " + exitValue);
            return gson.toJson(resultMapping);
        }
        catch(Exception e){
            return null;
        }
    }

    public String listSources(long uid){
        try{
            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/info " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location") + " -r " + uid;
            System.out.println("BTDataStore: running with command: " + launchCommand);

            Runtime rt = Runtime.getRuntime();

            //create process for operation
            final Process pr = rt.exec(launchCommand);


            new Thread(){//outputs the errorstream
                public void run(){
                    BufferedReader error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line=null;
                    try{
                        while((line=error.readLine()) != null) { //output all console output from the execution
                            System.out.println("BTDataStore-error: " + line);
                        }
                    } catch(Exception e){}
                }

            }.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line=null;

            String result = "";

            while((line=input.readLine()) != null) { //output all console output from the execution
                System.out.println("BTDataStore: " + line);
                result += line;
            }

            channelInfoResponse infoResponse = gson.fromJson(result,channelInfoResponse.class);

            SourcesResponse response = new SourcesResponse(infoResponse);

            //get the exit value
            int exitValue = pr.waitFor();
            System.out.println("BTDataStore: exited with code " + exitValue);
            return gson.toJson(response);
        }
        catch(Exception e){
            return gson.toJson(new SourcesResponse(null));
        }
    }

    private static class GetTileResponse{
        Object[][] data;
        String[] fields;
        int level;
        int offset;
        int sample_width;


        void putIntoMap(Map<String,Object> map){
            map.put("data",data);
            map.put("fields",fields);
            map.put("level",level);
            map.put("offset",offset);
            map.put("sample_width",sample_width);
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
        ArrayList<Style> styles;

        public static ChannelStyle getDefaultChannelStyle(String name){
            ChannelStyle style = new ChannelStyle();
            style.styles = new ArrayList<Style>();
             if (name.equals("Sleep_Graph")){
                 Style subStyle = new Style();
                 subStyle.type = "Zeo";
                 style.styles.add(subStyle);
             }
            else{
                 Style subStyle = new Style();
                 subStyle.type = "line";
                 subStyle.lineWidth = 1;
                 style.styles.add(subStyle);
             }
            return style;
        }

    }

    private static class Style{
        String type;
        int lineWidth;
    }

}
