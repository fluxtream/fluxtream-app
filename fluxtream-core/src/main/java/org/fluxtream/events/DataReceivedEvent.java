package org.fluxtream.events;

import java.util.List;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.connectors.updaters.UpdateInfo;
import org.fluxtream.domain.AbstractFacet;
import org.fluxtream.domain.Event;

/**
 *
 * @author Candide Kemmler (candide@fluxtream.com)
 */
public class DataReceivedEvent extends Event {

    public List<ObjectType> objectTypes;
    public String date;

    public long start, end;
    List<AbstractFacet> facets;
    public UpdateInfo updateInfo;

    public DataReceivedEvent(UpdateInfo updateInfo, List<ObjectType> objectTypes, long start, long end,
                             List<AbstractFacet> facets) {
        this.updateInfo = updateInfo;
        this.objectTypes = objectTypes;
        this.start = start;
        this.end = end;
        this.facets = facets;
    }

}
