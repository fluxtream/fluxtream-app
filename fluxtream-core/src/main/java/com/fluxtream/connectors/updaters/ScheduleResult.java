package com.fluxtream.connectors.updaters;

import com.google.gson.annotations.Expose;

public class ScheduleResult {
	
	public ScheduleResult(ResultType resultType) {
		type = resultType;
	}
	
	public ScheduleResult() {}

    @Expose
	public ResultType type = ResultType.NO_RESULT;

	public enum ResultType {
		NO_RESULT, ALREADY_SCHEDULED,
			SCHEDULED_UPDATE_DEFERRED, SCHEDULED_UPDATE_IMMEDIATE
	}

}
