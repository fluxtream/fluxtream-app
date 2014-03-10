package org.fluxtream.domain;

import javax.persistence.Entity;

@Entity(name="DataUpdate")
public class DataUpdate extends AbstractEntity {
    public enum UpdateType{
        bodytrackData,
        bodytrackStyle,
        apiData,
        notification,
        intercom,
        delete
    }

    public DataUpdate(){}

    public UpdateType type;

    public long guestId;

    public long apiKeyId;//always at least needs to be associated with fluxtream capture so this can't be null
    public Long objectTypeId;

    public String channelNames;//format: device.[channelNames]

    public String additionalInfo;

    public long timestamp;

    public Long startTime;
    public Long endTime;


}
