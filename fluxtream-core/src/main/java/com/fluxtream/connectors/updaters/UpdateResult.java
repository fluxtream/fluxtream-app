package com.fluxtream.connectors.updaters;

public class UpdateResult {

	UpdateResult(ResultType resultType) {
		type = resultType;
	}

    public UpdateResult() {}

	public ResultType type = ResultType.NO_RESULT;

    public transient String stackTrace;

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
        return new UpdateResult(ResultType.UPDATE_SUCCEEDED);
	}

	public static UpdateResult rateLimitReachedResult() {
        return new UpdateResult(ResultType.HAS_REACHED_RATE_LIMIT);
	}
}