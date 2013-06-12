package com.fluxtream.connectors.updaters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fluxtream.TimeInterval;
import com.fluxtream.connectors.ObjectType;
import com.fluxtream.domain.ApiKey;

public class UpdateInfo implements Cloneable {

	public ApiKey apiKey;
	public TimeInterval timeInterval;
	public String jsonParams;
	public int objectTypes;
	
	private transient Map<String,Object> context;
	
	public enum UpdateType {
		NOOP_UPDATE,
		TIME_INTERVAL_UPDATE,
		INITIAL_HISTORY_UPDATE,
		INCREMENTAL_UPDATE,
		PUSH_TRIGGERED_UPDATE
	}
	
	UpdateType updateType;
	
	UpdateInfo(ApiKey apiKey) {this.apiKey = apiKey;}
	
	public List<ObjectType> objectTypes() {
		List<ObjectType> connectorTypes = ObjectType.getObjectTypes(apiKey.getConnector(), objectTypes);
		return connectorTypes;
	}

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UpdateInfo)) return false;
        UpdateInfo other = (UpdateInfo) o;
        boolean sameUser = other.apiKey.getGuestId() == apiKey.getGuestId();
        if (!sameUser) return false;
        boolean sameData =  other.apiKey.getConnector().getName().equals(apiKey.getConnector().getName())
                && other.objectTypes == objectTypes;
        if (!sameData) return false;
        boolean sameTimeInterval = timeInterval==null
                                 ? other.timeInterval==null
                                 : other.timeInterval.getStart() == timeInterval.getStart() && other.timeInterval.getEnd() == timeInterval.getStart();
        if (!sameTimeInterval) return false;
        boolean sameUpdateType = updateType == other.updateType;
        if (!sameUpdateType) return false;
        return true;
    }
	
	public long getGuestId() {
		return apiKey.getGuestId();
	}
	
	public TimeInterval getTimeInterval() {
		return timeInterval;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}

	public static final UpdateInfo noopUpdateInfo(ApiKey apiKey, int objectTypes) {
		UpdateInfo updateInfo = new UpdateInfo(apiKey);
		updateInfo.updateType = UpdateType.NOOP_UPDATE;
		updateInfo.objectTypes = objectTypes;
		return updateInfo;
	}
	
	public static final UpdateInfo initialHistoryUpdateInfo(ApiKey apiKey, int objectTypes) {
		UpdateInfo updateInfo = new UpdateInfo(apiKey);
		updateInfo.updateType = UpdateType.INITIAL_HISTORY_UPDATE;
		updateInfo.objectTypes = objectTypes;
		return updateInfo;
	}
	
	public static final UpdateInfo pushTriggeredUpdateInfo(ApiKey apiKey, int objectTypes, String...jsonParams) {
		UpdateInfo updateInfo = new UpdateInfo(apiKey);
		updateInfo.updateType = UpdateType.PUSH_TRIGGERED_UPDATE;
		updateInfo.objectTypes = objectTypes;
		if (jsonParams!=null&&jsonParams.length>0)
			updateInfo.jsonParams = jsonParams[0];
		return updateInfo;
	}
	
	public static final UpdateInfo refreshTimeIntervalUpdateInfo(ApiKey apiKey, int objectTypes, TimeInterval timeInterval) {
		UpdateInfo updateInfo = new UpdateInfo(apiKey);
		updateInfo.updateType = UpdateType.TIME_INTERVAL_UPDATE;
		updateInfo.timeInterval = timeInterval;
		updateInfo.objectTypes = objectTypes;
		return updateInfo;
	}
	
	public static final UpdateInfo IncrementalUpdateInfo(ApiKey apiKey, int objectTypes) {
		UpdateInfo updateInfo = new UpdateInfo(apiKey);
		updateInfo.updateType = UpdateType.INCREMENTAL_UPDATE;
		updateInfo.objectTypes = objectTypes;
		return updateInfo;
	}

	public void setContext(String key, Object value) {
		if (context==null) context = new HashMap<String,Object>();
		context.put(key, value);
	}
	
	public Object getContext(String key) {
		return (context==null) ? null : context.get(key);
	}
	
}
