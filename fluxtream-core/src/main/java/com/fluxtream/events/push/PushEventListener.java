package com.fluxtream.events.push;

import com.fluxtream.events.EventListener;
import com.fluxtream.services.EventListenerService;
import com.fluxtream.utils.HttpUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A Listener for Push Connector events
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class PushEventListener implements EventListener<PushEvent> {

    static Logger logger = Logger.getLogger(PushEventListener.class);

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

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
        HttpUtils.post(invokeUrl, event.json);
    }

}
