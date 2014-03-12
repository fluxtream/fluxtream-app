package org.fluxtream.services;

import java.util.List;
import org.fluxtream.domain.DataUpdate;

/**
 * Created by justin on 3/10/14.
 */
public interface DataUpdateService {
    public void logBodyTrackDataUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName,
                                       String[] channelNames, long startTime, long endTime);
    public void logBodyTrackStyleUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName,
                                        String[] channelNames);

    public List<DataUpdate> getAllUpdatesSince(long guestId, long sinceTime);
}
