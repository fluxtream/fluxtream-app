package com.fluxtream.events.push;

import java.io.IOException;
import com.fluxtream.connectors.Connector;
import com.fluxtream.events.EventListener;
import com.fluxtream.services.ConnectorUpdateService;
import com.fluxtream.services.EventListenerService;
import com.fluxtream.utils.HttpUtils;
import com.fluxtream.utils.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * A Listener for Push Connector events
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class PushEventListener implements EventListener<PushEvent> {

    static Logger logger = Logger.getLogger(PushEventListener.class);

    public String url;

    @Autowired
    final protected void setEventService(@Qualifier("eventListenerServiceImpl") EventListenerService evl) {
        evl.addEventListener(PushEvent.class, this);
    }

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
