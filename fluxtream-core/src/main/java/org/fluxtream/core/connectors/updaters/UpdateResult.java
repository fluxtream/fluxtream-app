package org.fluxtream.core.connectors.updaters;

import org.fluxtream.core.domain.ApiKey;
import org.fluxtream.core.utils.Utils;

import static org.fluxtream.core.utils.Utils.stackTrace;

public class UpdateResult {


	private UpdateResult(ResultType resultType) {
		type = resultType;
	}

    public ResultType getType() {
        return type;
    }

    private ResultType type = ResultType.NO_RESULT;

    public transient String stackTrace;

    public String reason;

    private AuthRevokedException authRevokedException;

    public AuthRevokedException getAuthRevokedException() {
        return authRevokedException;
    }

    public enum ResultType {
		NO_RESULT, UPDATE_SUCCEEDED, UPDATE_FAILED, HAS_REACHED_RATE_LIMIT,
			DUPLICATE_UPDATE, UPDATE_FAILED_PERMANENTLY, NEEDS_REAUTH,
        AUTH_REVOKED
	}

    // Failusre can either be transient or permanent.  Default to transient, but allow optional
    // second arg to allow setting permanent if true
    public static UpdateResult failedResult(final String stackTrace, final String reason) {
        return(failedResult(stackTrace, false, reason));
    }

    public static UpdateResult failedResult(UpdateFailedException e) {
        return failedResult(stackTrace(e), e.isPermanent(), e.getReason());
    }

	public static UpdateResult failedResult(String stackTrace, boolean permanentFailure, String reason) {
        ResultType resultType = permanentFailure ? ResultType.UPDATE_FAILED_PERMANENTLY : ResultType.UPDATE_FAILED;
		UpdateResult updateResult = new UpdateResult(resultType);
		updateResult.stackTrace = stackTrace;
        updateResult.reason = reason;
		return updateResult;
	}

	public static UpdateResult successResult() {
        return new UpdateResult(ResultType.UPDATE_SUCCEEDED);
	}

    public static UpdateResult authRevokedResult(AuthRevokedException authRevokedException) {
        final UpdateResult updateResult = new UpdateResult(ResultType.AUTH_REVOKED);
        updateResult.authRevokedException = authRevokedException;
        updateResult.stackTrace = Utils.stackTrace(authRevokedException);
        updateResult.reason = new StringBuffer(ApiKey.PermanentFailReason.AUTH_REVOKED)
                .append(ApiKey.PermanentFailReason.DIVIDER)
                .append("dataCleanupRequested=")
                .append(authRevokedException.isDataCleanupRequested()).toString();
        return updateResult;
    }

    public static UpdateResult needsReauth() {
        final UpdateResult updateResult = new UpdateResult(ResultType.NEEDS_REAUTH);
        updateResult.reason = ApiKey.PermanentFailReason.NEEDS_REAUTH;
        return updateResult;
    }

	public static UpdateResult rateLimitReachedResult(RateLimitReachedException rateLimitReachedException) {
        final UpdateResult updateResult = new UpdateResult(ResultType.HAS_REACHED_RATE_LIMIT);
        updateResult.stackTrace = Utils.stackTrace(rateLimitReachedException);
        return updateResult;
    }
}