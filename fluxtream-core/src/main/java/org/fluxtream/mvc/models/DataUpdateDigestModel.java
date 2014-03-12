package org.fluxtream.mvc.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fluxtream.domain.DataUpdate;

public class DataUpdateDigestModel {
    Map<String,Map<String,TimeBoundariesModel>> bodytrackData;//a list of all the bodytrack data requests
    Map<String,Set<String>> bodytrackStyle;

    long generationTimestamp;

    public DataUpdateDigestModel(List<DataUpdate> updates) throws Exception{
        generationTimestamp = System.currentTimeMillis();
        for (DataUpdate update : updates){
            switch (update.type){
                case bodytrackData:
                    addBodytrackDataUpdate(update);
                    break;
                case bodytrackStyle:
                    addBodytrackStyleUpdate(update);
                    break;
                default:
                    throw new Exception("Unhandled update type encountered!");
            }
        }
    }

    private void addBodytrackDataUpdate(DataUpdate update){
        if (bodytrackData == null){
            bodytrackData = new HashMap<String,Map<String,TimeBoundariesModel>>();
        }
        String[] mainParts = update.channelNames.split("\\.");
        String deviceName = mainParts[0];
        String[] channelNames = mainParts[1].substring(1,mainParts[1].length() - 1).split(",");
        Map<String,TimeBoundariesModel> deviceMap = bodytrackData.get(deviceName);
        if (deviceMap == null){
            deviceMap = new HashMap<String,TimeBoundariesModel>();
            bodytrackData.put(deviceName,deviceMap);
        }
        for (String channelName : channelNames){
            deviceMap.put(channelName, new TimeBoundariesModel(update.startTime,update.endTime));
        }
    }

    private void addBodytrackStyleUpdate(DataUpdate update){
        if (bodytrackStyle == null){
            bodytrackStyle = new HashMap<String,Set<String>>();
        }
        String[] mainParts = update.channelNames.split("\\.");
        String deviceName = mainParts[0];
        String[] channelNames = mainParts[1].substring(1,mainParts[1].length() - 1).split(",");
        Set<String> channelSet = bodytrackStyle.get(deviceName);
        if (channelSet == null){
            channelSet = new HashSet<String>();
            bodytrackStyle.put(deviceName,channelSet);

        }
        Collections.addAll(channelSet, channelNames);
    }

}
