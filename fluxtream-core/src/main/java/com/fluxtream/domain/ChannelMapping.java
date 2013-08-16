package com.fluxtream.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="ChannelMapping")
@NamedQueries({
      @NamedQuery(name="channelMapping.byApiKeyID",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.guestId=? AND mapping.apiKeyId=?")
})
public class ChannelMapping extends AbstractEntity {
    @Index(name = "apiKey")
    public Long apiKeyId;

    @Index(name="guestId")
    public Long guestId;

    public Long objectTypeId;

    public String deviceName;
    public String channelName;

    public enum ChannelType {data,timespan,photo};

    public ChannelType channelType;

}
