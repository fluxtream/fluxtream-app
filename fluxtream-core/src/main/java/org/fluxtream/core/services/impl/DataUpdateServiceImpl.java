package org.fluxtream.core.services.impl;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.core.domain.DataUpdate;
import org.fluxtream.core.services.DataUpdateService;
import org.fluxtream.core.utils.JPAUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataUpdateServiceImpl implements DataUpdateService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = false)
    private void createDataUpdate(DataUpdate.UpdateType type, long guestId, Long apiKeyId, Long objectTypeId,
                                  String deviceName,  String[] channelNames, String addtionalInfo, Long start, Long end){
        DataUpdate update = new DataUpdate();
        update.type = type;
        update.guestId = guestId;
        update.apiKeyId = apiKeyId;
        update.objectTypeId = objectTypeId;
        if (channelNames != null){
            StringBuilder channelNamesBuilder = new StringBuilder(deviceName == null ? "" : deviceName).append(".[");
            channelNames = channelNames.clone();
            Arrays.sort(channelNames);//sort so that channel names strings are the same for the same set of channels
            for (int i = 0; i < channelNames.length; i++){
                if (i != 0){
                    channelNamesBuilder.append(",");
                }
                channelNamesBuilder.append(channelNames[i]);
            }
            update.channelNames = channelNamesBuilder.append(']').toString();
        }
        else{
            update.channelNames = deviceName;
        }
        update.additionalInfo = addtionalInfo;
        update.startTime = start;
        update.endTime = end;
        update.timestamp = System.currentTimeMillis();
        em.persist(update);
    }

    @Override
    @Transactional(readOnly = false)
    public void logBodyTrackDataUpdate(final long guestId, final long apiKeyId, final Long objectTypeId, final String deviceName, String[] channelNames, final long startTime, final long endTime) {
        createDataUpdate(DataUpdate.UpdateType.bodytrackData, guestId, apiKeyId, objectTypeId, deviceName, channelNames, null, startTime, endTime);

    }

    @Override
    @Transactional(readOnly = false)
    public void logBodyTrackDataUpdate(final long guestId, final long apiKeyId, final Long objectTypeId, final BodyTrackHelper.ParsedBodyTrackUploadResult parsedResult) {
        if (!parsedResult.isSuccess() || parsedResult.getParsedResponse().channel_specs == null)
            return;
        String deviceName = parsedResult.getDeviceName();
        String[] channels = parsedResult.getParsedResponse().channel_specs.keySet().toArray(new String[]{});
        long startTime = (long) (parsedResult.getParsedResponse().min_time * 1000);
        long endTime = (long) (parsedResult.getParsedResponse().max_time * 1000);
        logBodyTrackDataUpdate(guestId, apiKeyId, objectTypeId, deviceName, channels, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = false)
    public void logBodyTrackStyleUpdate(final long guestId, final long apiKeyId, final Long objectTypeId, final String deviceName, final String[] channelNames) {
        createDataUpdate(DataUpdate.UpdateType.bodytrackStyle, guestId, apiKeyId, objectTypeId, deviceName, channelNames, null, null, null);
    }

    @Override
    @Transactional(readOnly = false)
    public void logNotificationUpdate(final long guestId){
        createDataUpdate(DataUpdate.UpdateType.notification,guestId,null,null,null,null,null,null,null);
    }

    @Override
    public void logApiDataUpdate(final long guestId, final long apiKeyId, final Long objectTypeId, final long startTime, final long endTime) {
        createDataUpdate(DataUpdate.UpdateType.apiData,guestId,apiKeyId,objectTypeId,null,null,null,startTime,endTime);
    }

    @Override
    public void logBodyTrackDataUpdate(long guestId, long apiKeyId, Long objectTypeId, String deviceName, String[] channelNames, String additionalInfo) {
        createDataUpdate(DataUpdate.UpdateType.apiData,guestId,apiKeyId,objectTypeId,deviceName,channelNames,additionalInfo,null,null);
    }

    @Override
    public List<DataUpdate> getAllUpdatesSince(final long guestId, final long sinceTime) {
        return JPAUtils.find(em,DataUpdate.class,"dataUpdate.since",guestId,sinceTime);
    }

    @Override
    @Transactional(readOnly = false)
    public void cleanupOldDataUpdates() {
        JPAUtils.execute(em,"dataUpdate.delete.before",System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    }
}
