package org.fluxtream.connectors.updaters;

import javax.persistence.PersistenceException;

/**
 * <p>
 * <code>UpdateFailedException</code> does something...
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class UpdateFailedException extends Exception {
    private boolean isPermanent;

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

    public UpdateFailedException(boolean permanentFailure) {
        super();
        isPermanent=permanentFailure;
    }

    public UpdateFailedException(String message, Throwable cause, boolean permanentFailure) {
        super(message, cause);
        isPermanent=permanentFailure;
    }

    public UpdateFailedException(String message, boolean permanentFailure) {
        super(message);
        isPermanent=permanentFailure;
    }

    public UpdateFailedException(Throwable cause, boolean permanentFailure) {
        super(cause);
        isPermanent=permanentFailure;
    }

    public boolean isPermanent() {
        final Throwable cause = getCause();
        if (cause !=null) {
            // typical internal errors that will consistently crash the udpate
            if (cause instanceof PersistenceException)
                return true;
            String className = cause.getClass().toString();
            if (className.startsWith("org.springframework"))
                return true;
        }
        return isPermanent;
    }
}
