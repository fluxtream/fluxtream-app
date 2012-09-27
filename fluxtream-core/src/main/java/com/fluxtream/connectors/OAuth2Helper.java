package com.fluxtream.connectors;

import java.io.IOException;
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
public class OAuth2Helper {

    @Autowired
    Configuration env;

    Logger logger = Logger.getLogger(OAuth2Helper.class);

    public boolean revokeRefreshToken(long guestId, Connector connector,
                                      String removeRefreshTokenURL) {
        try {
            HttpUtils.fetch(removeRefreshTokenURL);
        }
        catch (IOException e) {
            logger.error("Could not revoke token for user "
                         + guestId + ", connector " + connector.getName(), e);
            return false;
        }
        return true;
    }
}
