package com.fluxtream.events;

import java.util.List;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.Event;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class DataReceivedEvent extends Event {

    public long guestId;
    public Connector connector;
    public List<ObjectType> objectTypes;
    public String date;

    public long start, end;

    public DataReceivedEvent(long guestId, Connector connector, List<ObjectType> objectTypes, String date) {
        this.guestId = guestId;
        this.connector = connector;
        this.objectTypes = objectTypes;
        this.date = date;
    }

    public DataReceivedEvent(long guestId, Connector connector, List<ObjectType> objectTypes, long start, long end) {
        this.guestId = guestId;
        this.connector = connector;
        this.objectTypes = objectTypes;
        this.start = start;
        this.end = end;
    }

}
