package org.fluxtream.connectors.updaters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluxtream.TimeInterval;
import org.fluxtream.connectors.ObjectType;
import org.fluxtream.domain.ApiKey;

public class UpdateInfo implements Cloneable {

	public ApiKey apiKey;
	public TimeInterval timeInterval;
	public String jsonParams;
	public int objectTypes;
	
	private transient Map<String,Object> context;
    private transient Map<String,Integer> remainingApiCalls;
    private transient Map<String,Long> resetTimes;

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

    public void setRemainingAPICalls(String methodName, int remaining) {
        if (remainingApiCalls==null) remainingApiCalls = new HashMap<String, Integer>();
        remainingApiCalls.put(methodName, remaining);
    }

    public Integer getRemainingAPICalls(String methodName) {
        if (remainingApiCalls==null) return null;
        return remainingApiCalls.get(methodName);
    }

    public void setResetTime(String methodName, long resetTime) {
        if (resetTimes==null) resetTimes = new HashMap<String,Long>();
        resetTimes.put(methodName, resetTime);
    }

    /**
     * of all the reset times that have been collected during an update,
     * return the one that is farthest away in the future
     * @return time in millis (usually in the future)
     */
    public Long getSafeResetTime() {
        if (resetTimes==null||resetTimes.size()==0) return null;
        long resetTime = Long.MIN_VALUE;
        for (Long aLong : resetTimes.values()) {
            if (aLong>resetTime)
                resetTime = aLong;
        }
        return resetTime;
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
