package org.fluxtream.core.connectors.updaters;

import org.fluxtream.core.utils.Utils;

import static org.fluxtream.core.utils.Utils.stackTrace;

public class UpdateResult {

    RateLimitReachedException rateLimitReachedException;

	private UpdateResult(ResultType resultType) {
		type = resultType;
	}

    public UpdateResult() {}

    public ResultType getType() {
        return type;
    }

    private ResultType type = ResultType.NO_RESULT;

    public transient String stackTrace;

    public enum ResultType {
		NO_RESULT, UPDATE_SUCCEEDED, UPDATE_FAILED, HAS_REACHED_RATE_LIMIT,
			DUPLICATE_UPDATE, UPDATE_FAILED_PERMANENTLY, NEEDS_REAUTH
	}

    // Failusre can either be transient or permanent.  Default to transient, but allow optional
    // second arg to allow setting permanent if true
    public static UpdateResult failedResult(String stackTrace) {
        return(failedResult(stackTrace,false));
    }

    public static UpdateResult failedResult(UpdateFailedException e) {
        return failedResult(stackTrace(e),e.isPermanent);
    }

	public static UpdateResult failedResult(String stackTrace, boolean permanentFailure) {
        ResultType resultType = permanentFailure ? ResultType.UPDATE_FAILED_PERMANENTLY : ResultType.UPDATE_FAILED;
		UpdateResult updateResult = new UpdateResult(resultType);
		updateResult.stackTrace = stackTrace;
		return updateResult;
	}

	public static UpdateResult successResult() {
        return new UpdateResult(ResultType.UPDATE_SUCCEEDED);
	}

    public static UpdateResult needsReauth() {
        return new UpdateResult(ResultType.NEEDS_REAUTH);
    }

	public static UpdateResult rateLimitReachedResult(RateLimitReachedException rateLimitReachedException) {
        final UpdateResult updateResult = new UpdateResult(ResultType.HAS_REACHED_RATE_LIMIT);
        updateResult.rateLimitReachedException = rateLimitReachedException;
        updateResult.stackTrace = Utils.stackTrace(rateLimitReachedException);
        return updateResult;
    }
}