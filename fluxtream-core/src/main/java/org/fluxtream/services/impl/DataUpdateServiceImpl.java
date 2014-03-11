package org.fluxtream.services.impl;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.domain.DataUpdate;
import org.fluxtream.services.DataUpdateService;
import org.fluxtream.utils.JPAUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataUpdateServiceImpl implements DataUpdateService {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = false)
    public void logBodyTrackDataUpdate(final long guestId, final long apiKeyId, final Long objectTypeId, final String deviceName, String[] channelNames, final long startTime, final long endTime) {
        DataUpdate update = new DataUpdate();
        update.guestId = guestId;
        update.type = DataUpdate.UpdateType.bodytrackData;
        update.apiKeyId = apiKeyId;
        update.objectTypeId = objectTypeId;
        update.additionalInfo = null;
        StringBuilder channelNamesBuilder = new StringBuilder(deviceName).append(".[");
        channelNames = channelNames.clone();
        Arrays.sort(channelNames);//sort so that channel names strings are the same for the same set of channels
        for (int i = 0; i < channelNames.length; i++){
            if (i != 0){
                channelNamesBuilder.append(",");
            }
            channelNamesBuilder.append(channelNames[i]);
        }
        update.channelNames = channelNamesBuilder.append(']').toString();
        update.startTime = startTime;
        update.endTime = endTime;
        update.timestamp = System.currentTimeMillis();
        em.persist(update);

    }

    @Override
    public List<DataUpdate> getAllUpdatesSince(final long guestId, final long sinceTime) {
        return JPAUtils.find(em,DataUpdate.class,"dataUpdate.since",guestId,sinceTime);
    }
}
