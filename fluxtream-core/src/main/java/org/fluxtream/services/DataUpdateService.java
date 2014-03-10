package org.fluxtream.services;

/**
 * Created by justin on 3/10/14.
 */
public interface DataUpdateService {
    public void logBodyTrackDataUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName,
                                       String[] channelNames, long startTime, long endTime);
}
