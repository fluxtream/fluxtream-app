package org.fluxtream.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.fluxtream.aspects.FlxLogger;
import org.fluxtream.connectors.Connector;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.AbstractLocalTimeFacet;
import org.fluxtream.domain.Guest;
import org.fluxtream.services.EventListenerService;
import org.fluxtream.services.GuestService;
import org.fluxtream.utils.Parse;
import org.fluxtream.utils.RestCallException;
import org.fluxtream.utils.parse.FacetCreatedEvent;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DataReceivedEventListener implements EventListener<DataReceivedEvent> {

    static FlxLogger logger = FlxLogger.getLogger(DataReceivedEventListener.class);

    @Autowired
    GuestService guestService;

    @Autowired
    Parse parse;

    DateTimeFormatter utcTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ");

    @Autowired
    final protected void setEventService(@Qualifier("eventListenerServiceImpl") EventListenerService evl) {
        StringBuilder sb = new StringBuilder("module=events component=DataReceivedEventListener action=setEventService")
                .append(" message=\"registering event listener\"");
        logger.info(sb.toString());
        evl.addEventListener(DataReceivedEvent.class, this);
    }

    @Override
    public void handleEvent(final DataReceivedEvent event) {
        // ignore history updates
        if (event.updateInfo.getUpdateType()== UpdateInfo.UpdateType.INITIAL_HISTORY_UPDATE)
            return;
        // ignore if no parse config present
        if (!parse.isParseConfigurationPresent())
            return;
        // ignore if guestId is not in parse list
        if (!parse.isInParseGuestList(event.updateInfo.getGuestId()))
            return;
        final StringBuilder msgAtts = new StringBuilder("module=events component=DataReceivedEventListener action=handleEvent");
        final Connector connector = event.updateInfo.apiKey.getConnector();
        final String connectorName = connector.getName();
        final Guest guest = guestService.getGuestById(event.updateInfo.getGuestId());
        final StringBuilder sb = new StringBuilder(msgAtts)
                .append(" connector=").append(connectorName)
                .append(" eventType=").append(event.objectTypes)
                .append(" date=").append(event.date)
                .append(" guestId=").append(event.updateInfo.getGuestId());
        for (AbstractFacet facet : event.facets) {
            final FacetCreatedEvent facetCreatedEvent = new FacetCreatedEvent();
            facetCreatedEvent.username = guest.username;
            facetCreatedEvent.serverName = parse.getServerName();
            facetCreatedEvent.connectorName = connectorName;
            facetCreatedEvent.objectType = ObjectType.getObjectType(connector, facet.objectType).getName();
            facetCreatedEvent.isLocalTime = facet instanceof AbstractLocalTimeFacet;
            if (facet instanceof AbstractLocalTimeFacet) {
                AbstractLocalTimeFacet localTimeFacet = (AbstractLocalTimeFacet)facet;
                facetCreatedEvent.date = localTimeFacet.date;
            }
            facetCreatedEvent.start = facet.start;
            facetCreatedEvent.end = facet.end;
            facetCreatedEvent.description = facet.fullTextDescription;
            Runnable parseLog = new Runnable() {
                public void run() {
                    try {
                        logger.info(sb.append(" message=\"logging to parse...\""));
                        parse.create("FacetCreatedEvent", facetCreatedEvent);
                    }
                    catch (RestCallException e) {
                        e.printStackTrace();
                    }
                }
            };
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(parseLog);
            executor.shutdown();
        }
    }

}
