package com.fluxtream.events.push;

import com.fluxtream.events.EventListener;
import com.fluxtream.services.EventListenerService;
import com.fluxtream.utils.HttpUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * A Listener for Push Connector events
 * @author Candide Kemmler (candide@fluxtream.com)
 */
@Component
public class PushEventListener implements EventListener<PushEvent> {

    static Logger logger = Logger.getLogger(PushEventListener.class);

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Autowired
    final protected void setEventService(@Qualifier("eventListenerServiceImpl") EventListenerService evl) {
        StringBuilder sb = new StringBuilder("module=events component=PushEventListener action=setEventService")
                .append(" message=\"registering event listener\"");
        logger.info(sb.toString());
        evl.addEventListener(PushEvent.class, this);
    }

    @Override
    public void handleEvent(final PushEvent event) {
        final StringBuilder msgAtts = new StringBuilder("module=events component=PushEventListener action=handleEvent")
                .append(" url=").append(this.url);
        StringBuilder sb = new StringBuilder(msgAtts)
                .append(" connector=").append(event.connectorName)
                .append(" eventType=").append(event.eventType)
                .append(" json=").append(event.json)
                .append(" guestId=").append(event.guestId);
        logger.info(sb.toString());
        String invokeUrl = url.replaceAll("\\$guestId", String.valueOf(event.guestId))
                .replaceAll("\\$connectorName", event.connectorName)
                .replaceAll("\\$eventType", event.eventType);
        logger.info(invokeUrl);
        HttpUtils.post(invokeUrl, event.json);
    }

}
