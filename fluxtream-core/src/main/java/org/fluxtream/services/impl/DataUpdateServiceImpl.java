package org.fluxtream.services.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.fluxtream.domain.DataUpdate;
import org.fluxtream.services.DataUpdateService;
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
    public void logBodyTrackDataUpdate(final long guestId, final long apiKeyId, final Long objectTypeId, final String deviceName, final String[] channelNames, final long startTime, final long endTime) {
        DataUpdate update = new DataUpdate();
        update.type = DataUpdate.UpdateType.bodytrackData;
        update.apiKeyId = apiKeyId;
        update.objectTypeId = objectTypeId;
        update.additionalInfo = null;
        StringBuilder channelNamesBuilder = new StringBuilder(deviceName).append(".[");
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
}
