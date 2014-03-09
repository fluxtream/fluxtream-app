package org.fluxtream.domain;

import javax.persistence.Entity;

@Entity(name="DataUpdate")
public class DataUpdate extends AbstractEntity {
    public enum UpdateType{
        bodytrack,
        apiData,
        notification,
        intercom
    }

    UpdateType type;

    long guestId;

    long apiKeyId;
    long objectTypeId;

    String additionalInfo;

    long timestamp;

    long startTime;
    long endTime;


}
