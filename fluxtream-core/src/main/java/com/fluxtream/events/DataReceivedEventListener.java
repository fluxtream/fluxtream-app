package com.fluxtream.events;

import com.fluxtream.connectors.ObjectType;
import com.fluxtream.services.EventListenerService;
import com.fluxtream.utils.HttpUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A Listener for Push Connector events whose handleEvent method calls an external URL
 * parameterized with the event's data
 * The URL will have to contain placeholders of the form <ul>
 *     <li>$guestId</li>
 *     <li>$connectorName</li>
 *     <li>$date</li>
 *     <li>$objectType</li>
 * </ul>
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class DataReceivedEventListener implements EventListener<DataReceivedEvent> {

    static Logger logger = Logger.getLogger(DataReceivedEventListener.class);

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Autowired
    final protected void setEventService(@Qualifier("eventListenerServiceImpl") EventListenerService evl) {
        StringBuilder sb = new StringBuilder("module=events component=DataReceivedEventListener action=setEventService")
                .append(" message=\"registering event listener\"");
        logger.info(sb.toString());
        evl.addEventListener(DataReceivedEvent.class, this);
    }

    @Override
    public void handleEvent(final DataReceivedEvent event) {
        final StringBuilder msgAtts = new StringBuilder("module=events component=DataReceivedEventListener action=handleEvent")
                .append(" url=").append(this.url);
        StringBuilder sb = new StringBuilder(msgAtts)
                .append(" connector=").append(event.connector.getName())
                .append(" eventType=").append(event.objectTypes)
                .append(" date=").append(event.date)
                .append(" guestId=").append(event.guestId);
        logger.info(sb.toString());
        for (ObjectType objectType : event.objectTypes) {
            String invokeUrl = url.replaceAll("\\$guestId", String.valueOf(event.guestId))
                    .replaceAll("\\$connectorName", event.connector.getName())
                    .replaceAll("\\$date", event.date)
                    .replaceAll("\\$objectType", objectType.getName());
            logger.info(invokeUrl);
            HttpUtils.post(invokeUrl, null);
        }
    }

}
