package org.fluxtream.core.services;

import java.util.List;
import org.fluxtream.core.domain.DataUpdate;
import org.fluxtream.core.services.impl.BodyTrackHelper;

/**
 * Created by justin on 3/10/14.
 */
public interface DataUpdateService {
    public void logBodyTrackDataUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName,
                                       String[] channelNames, long startTime, long endTime);
    public void logBodyTrackDataUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName,
                                       String[] channelNames, String additionalInfo);

    public void logBodyTrackDataUpdate(long guestId, long apiKeyId, Long objectTypeId, BodyTrackHelper.ParsedBodyTrackUploadResult uploadResult);
    public void logBodyTrackStyleUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName,
                                        String[] channelNames);
    public void logNotificationUpdate(final long guestId);

    public void logApiDataUpdate(long guestId, long apiKeyId, Long objectTypeId, long startTime, long endTime);

    public List<DataUpdate> getAllUpdatesSince(long guestId, long sinceTime);
    public void cleanupOldDataUpdates();
}
