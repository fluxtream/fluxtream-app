package org.fluxtream.mvc.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.fluxtream.connectors.updaters.ScheduleResult;
import org.fluxtream.connectors.updaters.UpdateResult;
import org.fluxtream.connectors.vos.AbstractFacetVO;
import org.fluxtream.domain.AbstractFacet;

public class ConnectorResponseModel {

	public TimeBoundariesModel tbounds;
	public List<UpdateResult> updateResults;
	public List<ScheduleResult> scheduleResults;
	public Map<String,Collection<AbstractFacetVO<AbstractFacet>>> facets;
	
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
