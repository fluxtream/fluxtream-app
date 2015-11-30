package org.fluxtream.core.domain;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity(name="ChannelMapping")
@NamedQueries({
      @NamedQuery(name="channelMapping.delete.all",
                  query="DELETE FROM ChannelMapping mapping WHERE mapping.guestId=?"),
      @NamedQuery(name="channelMapping.byApiKeyId",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.apiKeyId=?"),
      @NamedQuery(name="channelMapping.byApiKeyAndObjectType",
                  query="SELECT mapping FROM ChannelMapping mapping WHERE mapping.guestId=? AND mapping.apiKeyId=? AND mapping.objectTypes=?"),
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

    public Long getApiKeyId() {
        return apiKeyId;
    }

    @Index(name = "apiKey")
    Long apiKeyId = null;

    @Index(name="guestId")
    Long guestId = null;

    public Integer getObjectTypes() {
        return objectTypes;
    }

    Integer objectTypes = null;

    @Index(name="deviceName")
    String deviceName = null;

    public Long getGuestId() {
        return guestId;
    }

    public TimeType getTimeType() {
        return timeType;
    }

    public CreationType getCreationType() {
        return creationType;
    }

    public String getDeviceName() {

        return deviceName;
    }

    public String getChannelName() {
        return channelName;
    }

    @Index(name="channelName")
    String channelName = null;

    public void setInternalChannelName(String internalChannelName) {
        this.internalChannelName = internalChannelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public enum ChannelType {data,timespan,photo}

    public ChannelType getChannelType() {
        return channelType;
    }

    ChannelType channelType = null;

    public enum TimeType {gmt,local};

    TimeType timeType = null;

    public String getInternalDeviceName() {
        return internalDeviceName;
    }

    String internalDeviceName = null;

    public String getInternalChannelName() {
        return internalChannelName;
    }

    String internalChannelName = null;

    public void setCreationType(CreationType creationType) {
        this.creationType = creationType;
    }

    /**
     * Was this entry added during fixup, a history/incremental update or dynamically when data was uploaded
     */
    @Index(name="creationType")
    CreationType creationType = CreationType.legacy;
    public enum CreationType {legacy, fixUp, mapChannels, dynamic};

    public ChannelMapping(Long apiKeyId, Long guestId,
                          ChannelType channelType, TimeType timeType, Integer objectTypes,
                          String deviceName, String channelName,
                          String internalDeviceName, String internalChannelName) {
        this.apiKeyId = apiKeyId;
        this.guestId = guestId;
        this.objectTypes = objectTypes;
        this.deviceName = deviceName;
        this.channelName = channelName;
        this.channelType = channelType;
        this.timeType = timeType;
        this.internalDeviceName = internalDeviceName;
        this.internalChannelName = internalChannelName;
    }

    public ChannelMapping() {}

    /**
     * Adds a channelMapping with passed parameter and a gmt timeType, 'data' channelType to the supplied channelMappings list
     * @param apiKey
     * @param objectTypes
     * @param deviceName
     * @param channelName
     * @param channelMappings
     */
    public static void addToDeclaredMappings(ApiKey apiKey, Integer objectTypes,
                                             String deviceName, String channelName, List<ChannelMapping> channelMappings) {
        ChannelMapping mapping = new ChannelMapping(apiKey.getId(), apiKey.getGuestId(), ChannelType.data, TimeType.gmt, objectTypes,
                deviceName, channelName, deviceName, channelName);
        channelMappings.add(mapping);
    }
    public static void addToDeclaredMappings(ApiKey apiKey, ChannelType channelType, TimeType timeType, Integer objectTypes,
                                             String deviceName, String channelName, List<ChannelMapping> channelMappings) {
        ChannelMapping mapping = new ChannelMapping(apiKey.getId(), apiKey.getGuestId(), channelType, timeType, objectTypes,
                deviceName, channelName, deviceName, channelName);
        channelMappings.add(mapping);
    }

}
