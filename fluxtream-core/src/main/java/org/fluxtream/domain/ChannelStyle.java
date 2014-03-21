package org.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="ChannelStyle")
@NamedQueries({
      @NamedQuery(name="channelStyle.delete.all",
                  query="DELETE from ChannelStyle channelStyle WHERE channelStyle.guestId=?"),
      @NamedQuery(name="channelStyle.delete.byGuestAndDeviceName",
                  query="DELETE from ChannelStyle channelStyle WHERE channelStyle.guestId=? AND channelStyle.deviceName=?"),
      @NamedQuery(name="channelStyle.byDeviceNameAndChannelName",
                  query="SELECT style FROM ChannelStyle style WHERE style.guestId=? AND style.deviceName=? AND style.channelName=?")
})
public class ChannelStyle extends AbstractEntity {

    public ChannelStyle(){}

    @Index(name="guestId")
    public long guestId;
    @Index(name="deviceName")
    public String deviceName;
    @Index(name="channelName")
    public String channelName;
    @Lob
    public String json;
}
