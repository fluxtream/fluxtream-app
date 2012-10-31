package com.fluxtream.events.push;

import java.io.IOException;
import com.fluxtream.events.EventListener;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;
import org.apache.log4j.Logger;

/**
 * An eventListener
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class PushEventListener implements EventListener<PushEvent> {

    static Logger logger = Logger.getLogger(PushEventListener.class);

    public String url;

    @Override
    public void handleEvent(final PushEvent event) {
        StringBuilder sb = new StringBuilder("module=events component=PushEventListener action=handleEvent")
                .append(" connector=").append(event.connectorName)
                .append(" guestId=").append(event.guestId);
        logger.info(sb.toString());
        String invokeUrl = url.replaceAll("\\$guestId", String.valueOf(event.guestId))
                .replaceAll("\\$connectorName", event.connectorName)
                .replaceAll("\\$eventType", event.eventType);
        try {
            HttpUtils.fetch(invokeUrl);
        }
        catch (IOException e) {
            sb = new StringBuilder("module=events component=PushEventListener action=handleEvent")
                    .append(" connector=").append(event.connectorName)
                    .append(" guestId=").append(event.guestId)
                    .append(" stackTrace=<![CDATA[").append(Utils.stackTrace(e)).append("]]>");
            logger.warn(sb.toString());
        }
    }

}
