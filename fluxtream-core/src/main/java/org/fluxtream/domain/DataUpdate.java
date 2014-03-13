package org.fluxtream.domain;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.annotations.Index;

@Entity(name="DataUpdate")
@NamedQueries({
        @NamedQuery(name="dataUpdate.since",
            query="select new org.fluxtream.domain.DataUpdate(du.guestId, du.type, du.apiKeyId, du.objectTypeId, du.channelNames, du.additionalInfo, max(du.timestamp), min(du.startTime), max(du.endTime))" +
                  " from DataUpdate du where du.guestId = ? and du.timestamp >= ? group by du.guestId,du.type,du.apiKeyId,du.objectTypeId,du.channelNames,du.additionalInfo")
})
public class DataUpdate extends AbstractEntity {

    public DataUpdate(long guestId, UpdateType type, long apiKeyId, Long objectTypeId, String channelNames, String additionalInfo, long timestamp, Long startTime, Long endTime){
        this.guestId = guestId;
        this.type = type;
        this.apiKeyId = apiKeyId;
        this.objectTypeId = objectTypeId;
        this.channelNames = channelNames;
        this.additionalInfo = additionalInfo;
        this.timestamp = timestamp;
        this.startTime = startTime;
        this.endTime = endTime;
    }

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

    @Index(name="GuestIdAndTimestamp")
    public long guestId;

    public long apiKeyId;//always at least needs to be associated with fluxtream capture so this can't be null
    @Nullable
    public Long objectTypeId;

    @Nullable
    @Lob
    public String channelNames;//format: device.[channelNames]

    @Nullable
    @Lob
    public String additionalInfo;

    @Index(name="GuestIdAndTimestamp")
    public long timestamp;

    @Nullable
    public Long startTime;
    @Nullable
    public Long endTime;


}
