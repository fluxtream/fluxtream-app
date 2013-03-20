package com.fluxtream.events;

import java.util.Map;
import com.fluxtream.aspects.FlxLogger;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.services.EventListenerService;
import com.fluxtream.services.GuestService;
import com.fluxtream.utils.Parse;
import com.fluxtream.utils.parse.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DataReceivedEventListener implements EventListener<DataReceivedEvent> {

    static FlxLogger logger = FlxLogger.getLogger(DataReceivedEventListener.class);

    private Map<Long,User> parseUsers = new java.util.Hashtable<Long,User>();

    @Autowired
    GuestService guestService;

    @Autowired
    Parse parse;

    @Autowired
    final protected void setEventService(@Qualifier("eventListenerServiceImpl") EventListenerService evl) {
        StringBuilder sb = new StringBuilder("module=events component=DataReceivedEventListener action=setEventService")
                .append(" message=\"registering event listener\"");
        logger.info(sb.toString());
        evl.addEventListener(DataReceivedEvent.class, this);
    }

    @Override
    public void handleEvent(final DataReceivedEvent event) {
        // ignore if no parse config present
        if (!parse.isParseConfigurationPresent())
            return;
        // ignore if guestId is not in parse list
        if (getParseUser(event.guestId)==null)
            return;
        final StringBuilder msgAtts = new StringBuilder("module=events component=DataReceivedEventListener action=handleEvent");
        StringBuilder sb = new StringBuilder(msgAtts)
                .append(" connector=").append(event.connector.getName())
                .append(" eventType=").append(event.objectTypes)
                .append(" date=").append(event.date)
                .append(" guestId=").append(event.guestId);
        for (ObjectType objectType : event.objectTypes) {

        }
    }

    private User getParseUser(final long guestId) {
        // if user is not in parseList return null
        if (!parse.isInParseGuestList(guestId))
            return null;
        if (parseUsers.containsKey(guestId))
            return parseUsers.get(guestId);
        // hit https://api.parse.com/1/login with guest's secret
        return null;
    }
}
