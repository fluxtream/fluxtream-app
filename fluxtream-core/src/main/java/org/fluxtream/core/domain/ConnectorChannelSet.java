package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="ConnectorChannelSet")
@NamedQueries({
    @NamedQuery(name="connectorChannelSet.byApi",
                query="SELECT channelSet FROM ConnectorChannelSet channelSet WHERE channelSet.guestId=? AND channelSet.api=?")
})
public class ConnectorChannelSet extends AbstractEntity {

    public ConnectorChannelSet(){}

    @Index(name="guestId")
    public long guestId;
    @Index(name="api")
    public int api;
    @Lob
    public String channels;
}
