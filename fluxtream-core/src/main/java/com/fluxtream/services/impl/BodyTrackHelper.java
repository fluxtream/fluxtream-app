package com.fluxtream.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fluxtream.Configuration;
import com.fluxtream.utils.HttpUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
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

    public void uploadToBodyTrack(final String host, final String user_id, String deviceName, Collection<String> channelNames, List<List<Object>> data, Map<String,Collection<Object>> channelSpecs) {
        try {
            Map<String, String> params = new HashMap<String,String>();
            params.put("dev_nickname", deviceName);
            params.put("channel_names", gson.toJson(channelNames));
            params.put("channel_specs", gson.toJson(channelSpecs));
            params.put("data", gson.toJson(data));

            String result = HttpUtils.fetch("http://" + host + "/users/"
                                            + user_id + "/upload", params, env);
            if (result.toLowerCase().startsWith("awesome")) {
                LOG.info("Data successfully uploaded to BodyTrack: guestId: "
                         + user_id);
            } else {
                LOG.warn("Could not upload data to BodyTrack data store: "
                         + result);
            }

            //TODO: develop this with working datastore
            /*File tempFile = File.createTempFile("input",".json");
            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data",data);
            tempFileMapping.put("channel_names ",channelNames);
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(gson.toJson(tempFileMapping).getBytes());
            fos.close();
            Runtime rt = Runtime.getRuntime();


            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/import " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location") + " " + user_id + " " +
                                   deviceName + " \"" + tempFile.getAbsolutePath() + "\"";
            System.out.println("BTDataStore: running with command: " + launchCommand);

            //create process for operation
            Process pr = rt.exec(launchCommand);

            //BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            /*String line=null;

            while((line=input.readLine()) != null) { //output all console output from the execution
                System.out.println("BTDataStore: " + line);
            }

            //get the exit value
            int exitValue = pr.waitFor();
            System.out.println("BTDataStore: exited with code " + exitValue);
            tempFile.delete();  */
        } catch (Exception e) {
            LOG.warn("Could not upload data to BodyTrack data store: "
                     + e.getMessage());
        }
    }

    public void uploadToBodyTrack(final String user_id, final String host, final Map<String, String> params) {
        try {
            String result = HttpUtils.fetch("http://" + host + "/users/"
                                            + user_id + "/upload", params, env);
            if (result.toLowerCase().startsWith("awesome")) {
                LOG.info("Data successfully uploaded to BodyTrack: guestId: "
                         + user_id);
            } else {
                LOG.warn("Could not upload data to BodyTrack data store: "
                         + result);
            }

            //TODO: develop this with working datastore
            /*File tempFile = File.createTempFile("input",".json");
            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data",data);
            tempFileMapping.put("channel_names ",channelNames);
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(gson.toJson(tempFileMapping).getBytes());
            fos.close();
            Runtime rt = Runtime.getRuntime();


            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/import " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location") + " " + user_id + " " +
                                   deviceName + " \"" + tempFile.getAbsolutePath() + "\"";
            System.out.println("BTDataStore: running with command: " + launchCommand);

            //create process for operation
            Process pr = rt.exec(launchCommand);

            //BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            /*String line=null;

            while((line=input.readLine()) != null) { //output all console output from the execution
                System.out.println("BTDataStore: " + line);
            }

            //get the exit value
            int exitValue = pr.waitFor();
            System.out.println("BTDataStore: exited with code " + exitValue);
            tempFile.delete();  */
        } catch (Exception e) {
            LOG.warn("Could not upload data to BodyTrack data store: "
                     + e.getMessage());
        }
    }
}
