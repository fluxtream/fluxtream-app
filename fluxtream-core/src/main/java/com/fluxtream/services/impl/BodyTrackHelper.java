package com.fluxtream.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
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

    public void uploadToBodyTrack(final long guestId, final String user_id, final String host, final Map<String, String> params) {
        try {
            String result = HttpUtils.fetch("http://" + host + "/users/"
                                            + user_id + "/upload", params, env);
            if (result.toLowerCase().startsWith("awesome")) {
                LOG.info("Data successfully uploaded to BodyTrack: guestId: "
                         + guestId);
            } else {
                LOG.warn("Could not upload data to BodyTrack data store: "
                         + result);
            }

            File tempFile = File.createTempFile("input",".json");
            Map<String,Object> tempFileMapping = new HashMap<String,Object>();
            tempFileMapping.put("data",gson.fromJson(params.get("data"), long[].class));
            tempFileMapping.put("channel_names ",gson.fromJson(params.get("channel_names"),String[].class));
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(gson.toJson(tempFileMapping).getBytes());
            fos.close();
            Runtime rt = Runtime.getRuntime();


            String launchCommand = env.targetEnvironmentProps.getString("btdatastore.exec.location") + "/import " +
                                   env.targetEnvironmentProps.getString("btdatastore.db.location") + " " + user_id + " " +
                                   params.get("dev_nickname") + " \"" + tempFile.getAbsolutePath() + "\"";
            System.out.println("BTDataStore: running with command: " + launchCommand);

            //create process for operation
            Process pr = rt.exec(launchCommand);



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

}
