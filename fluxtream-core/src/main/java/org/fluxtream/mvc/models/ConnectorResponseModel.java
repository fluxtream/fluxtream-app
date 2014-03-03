package org.fluxtream.mvc.models;

import java.util.ArrayList;
import java.util.List;

import org.fluxtream.connectors.updaters.ScheduleResult;
import org.fluxtream.connectors.updaters.UpdateResult;

public class ConnectorResponseModel {

	public TimeBoundariesModel tbounds;
	public List<UpdateResult> updateResults;
	public List<ScheduleResult> scheduleResults;
	public Object payload;
	
	public void addScheduleResult(ScheduleResult scheduleResult) {
		if (scheduleResults==null) scheduleResults = new ArrayList<ScheduleResult>();
		scheduleResults.add(scheduleResult);
	}
	
	public void addUpdateResult(UpdateResult updateResult) {
		if (updateResults==null) updateResults = new ArrayList<UpdateResult>();
		updateResults.add(updateResult);
	}

	public boolean hasSuccessResult() {
		if (updateResults==null) return false;
		for (UpdateResult updateResult : updateResults) {
			if (updateResult.getType()==UpdateResult.ResultType.UPDATE_SUCCEEDED)
				return true;
		}
		return false;
	}
	
}
