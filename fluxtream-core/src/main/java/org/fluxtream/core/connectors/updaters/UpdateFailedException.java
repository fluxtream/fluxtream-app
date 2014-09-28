package org.fluxtream.core.connectors.updaters;

import javax.persistence.PersistenceException;
import org.fluxtream.core.domain.ApiKey;

/**
 * <p>
 * <code>UpdateFailedException</code> does something...
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class UpdateFailedException extends Exception {

    private boolean isPermanent;
    private String reason;

    public UpdateFailedException() {
        super();
        isPermanent=false;
    }

    public UpdateFailedException(String message, Throwable cause) {
        super(message, cause);
        isPermanent=false;
    }

    public UpdateFailedException(String message) {
        super(message);
        isPermanent=false;
    }

    public UpdateFailedException(Throwable cause) {
        super(cause);
        isPermanent=false;
    }

    public UpdateFailedException(boolean permanentFailure, String reason) {
        super();
        this.reason = reason;
        isPermanent=permanentFailure;
    }

    public UpdateFailedException(String message, Throwable cause, boolean permanentFailure, String reason) {
        super(message, cause);
        this.reason = reason;
        isPermanent=permanentFailure;
    }

    public UpdateFailedException(String message, boolean permanentFailure, String reason) {
        super(message);
        this.reason = reason;
        isPermanent=permanentFailure;
    }

    public UpdateFailedException(Throwable cause, boolean permanentFailure, String reason) {
        super(cause);
        this.reason = reason;
        isPermanent=permanentFailure;
    }

    public String getReason() {
        return reason;
    }

    public boolean isPermanent() {
        final Throwable cause = getCause();
        if (cause !=null) {
            // typical internal errors that will consistently crash the udpate
            String className = cause.getClass().toString();
            boolean serverException = false;
            if (cause instanceof PersistenceException) {
                serverException = true;
            }
            if (className.startsWith("org.springframework"))
                serverException = true;
            else if (className.startsWith("java.lang.NullPointerException"))
                serverException = true;

            if (serverException) {
                StringBuffer sb = new StringBuffer();
                if (reason!=null)
                    sb.append(reason).append(ApiKey.PermanentFailReason.DIVIDER).append(className);
                else
                    sb.append(ApiKey.PermanentFailReason.SERVER_EXCEPTION)
                        .append(ApiKey.PermanentFailReason.DIVIDER).append(className);
                this.reason = sb.toString();
                return true;
            }
        }
        return isPermanent;
    }
}
