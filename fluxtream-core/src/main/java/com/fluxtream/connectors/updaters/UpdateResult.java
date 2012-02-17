package com.fluxtream.connectors.updaters;

public class UpdateResult {

	UpdateResult(ResultType resultType) {
		type = resultType;
	}

	UpdateResult(ResultType resultType, String stackTrace) {
		type = resultType;
	}
	
	public UpdateResult() {}

	public ResultType type = ResultType.NO_RESULT;

	public enum ResultType {
		NO_RESULT, UPDATE_SUCCEEDED, UPDATE_FAILED, HAS_REACHED_RATE_LIMIT,
			DUPLICATE_UPDATE
	}
	
	public static UpdateResult failedResult(String stackTrace) {
		UpdateResult updateResult = new UpdateResult(ResultType.UPDATE_FAILED);
		updateResult.stackTrace = stackTrace;
		return updateResult;
	}
	
	public static UpdateResult successResult() {
		UpdateResult updateResult = new UpdateResult(ResultType.UPDATE_SUCCEEDED);
		return updateResult;
	}
	
	public static UpdateResult rateLimitReachedResult() {
		UpdateResult updateResult = new UpdateResult(ResultType.HAS_REACHED_RATE_LIMIT);
		return updateResult;
	}
	
	public static UpdateResult duplicateUpdateResult() {
		UpdateResult updateResult = new UpdateResult(ResultType.DUPLICATE_UPDATE);
		return updateResult;
	}
	
	public transient String stackTrace;

}
