package com.fluxtream.events.push;

import com.fluxtream.domain.Event;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class PushEvent extends Event {

    public long guestId;
    public String connectorName;
    public String eventType;

    public PushEvent(long guestId, String connectorName, String eventType) {
        this.guestId = guestId;
        this.connectorName = connectorName;
        this.eventType = eventType;
    }

}
