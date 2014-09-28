package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="ChannelMapping")
@NamedQueries({
      @NamedQuery(name="channelMapping.delete.all",
                  query="DELETE FROM ChannelMapping mapping WHERE mapping.guestId=?"),
      @NamedQuery(name="channelMapping.byApiKey",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.guestId=? AND mapping.apiKeyId=?"),
      @NamedQuery(name="channelMapping.byApiKeyAndObjectType",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.guestId=? AND mapping.apiKeyId=? AND mapping.objectTypeId=?"),
      @NamedQuery(name="channelMapping.byDisplayName",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.guestId=? AND mapping.deviceName=? AND mapping.channelName=?"),
      @NamedQuery(name="channelMapping.all",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.guestId=?"),
      @NamedQuery(name="channelMapping.delete",
                  query="DELETE FROM ChannelMapping mapping WHERE mapping.guestId=? AND mapping.apiKeyId=?"),
      @NamedQuery(name="channelMapping.delete.byApiKeyId",
                  query="DELETE FROM ChannelMapping mapping WHERE mapping.apiKeyId=?")
})
public class ChannelMapping extends AbstractEntity {
    @Index(name = "apiKey")
    public Long apiKeyId = null;

    @Index(name="guestId")
    public Long guestId = null;

    public Integer objectTypeId = null;

    @Index(name="deviceName")
    public String deviceName = null;
    @Index(name="channelName")
    public String channelName = null;

    public enum ChannelType {data,timespan,photo};

    public ChannelType channelType = null;

    public enum TimeType {gmt,local};

    public TimeType timeType = null;

    public String internalDeviceName = null;
    public String internalChannelName = null;

}
