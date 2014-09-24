package org.fluxtream.core.mvc.models;

import org.fluxtream.core.connectors.ObjectType;
import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.domain.DataUpdate;
import org.fluxtream.core.services.GuestService;
import org.fluxtream.core.services.SettingsService;
import org.joda.time.format.ISODateTimeFormat;

import java.util.*;

public class DataUpdateDigestModel {
    Map<String,Map<String,TimeBoundariesModel>> bodytrackData;//a list of all the bodytrack data requests
    Map<String,Map<String,TimeBoundariesModel>> apiData;
    Map<String,ConnectorDigestModel> connectorInfo;
    Map<String,Set<String>> bodytrackStyle;
    Boolean notification;

    String generationTimestamp;
    String queryTimestamp;

    public DataUpdateDigestModel(List<DataUpdate> updates, GuestService guestService, SettingsService settingsService, long sinceTime) throws Exception{
        queryTimestamp = ISODateTimeFormat.dateTime().print(sinceTime);
        generationTimestamp = ISODateTimeFormat.dateTime().print(System.currentTimeMillis());
        for (DataUpdate update : updates){
            switch (update.type){
                case bodytrackData:
                    addBodytrackDataUpdate(update);
                    break;
                case bodytrackStyle:
                    addBodytrackStyleUpdate(update);
                    break;
                case notification:
                    addNotificationUpdate(update);
                    break;
                case apiData:
                    addApiDataUpdate(update,guestService,settingsService);
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

    private void addApiDataUpdate(DataUpdate update, GuestService guestService, final SettingsService settingsService){
        ApiKey api = guestService.getApiKey(update.apiKeyId);
        if (api == null){
            //This means the specific apikeyid was removed
            return;
        }
        String apiName = api.getConnector().getName();
        List<String> facetNames = new ArrayList<String>();
        if (update.objectTypeId == null){     //TODO: determine if this is ever reached
            System.err.println("Unhandled: objectType = null for DataUpdate");
        }
        else{
            ObjectType[] objectTypes = api.getConnector().getObjectTypesForValue(update.objectTypeId.intValue());
            for(ObjectType objectType : objectTypes){
                facetNames.add(objectType.getName());
            }
        }
        if (facetNames.size() == 0){   //TODO: determine if this is ever reached
            System.err.println("ApiDataUpdate with no facetNames!");
            return;
        }
        if (apiData == null){
            apiData = new HashMap<String,Map<String,TimeBoundariesModel>>();
        }
        Map<String,TimeBoundariesModel> connectorMap = apiData.get(apiName);
        if (connectorMap == null){
            connectorMap = new HashMap<String,TimeBoundariesModel>();
            apiData.put(apiName,connectorMap);
        }
        for (String facetName : facetNames){
            connectorMap.put(facetName,new TimeBoundariesModel(update.startTime,update.endTime));
        }
        if (connectorInfo == null){
            connectorInfo = new HashMap<String,ConnectorDigestModel>();
        }
        if (connectorInfo.get(apiName) == null){
            ConnectorDigestModel model = new ConnectorDigestModel();
            model.apiKeyId = update.apiKeyId;
            model.channelNames = settingsService.getChannelsForConnector(api.getGuestId(),api.getConnector());
            model.prettyName = api.getConnector().getPrettyName();
            model.connectorName = api.getConnector().getName();
            for (ObjectType objectType : api.getConnector().objectTypes())
                model.facetTypes.add(model.connectorName + "-" + objectType.getName());
            connectorInfo.put(apiName,model);
        }

    }

    private void addNotificationUpdate(DataUpdate update){
        notification = true;
    }

}
