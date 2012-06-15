package com.fluxtream.services.impl;

import java.util.Map;
import com.fluxtream.Configuration;
import com.fluxtream.utils.HttpUtils;
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
        } catch (Exception e) {
            LOG.warn("Could not upload data to BodyTrack data store: "
                     + e.getMessage());
        }
    }

}
