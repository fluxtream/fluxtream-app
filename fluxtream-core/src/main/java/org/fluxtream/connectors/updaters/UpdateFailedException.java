package org.fluxtream.connectors.updaters;

/**
 * <p>
 * <code>UpdateFailedException</code> does something...
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class UpdateFailedException extends Exception {
    public boolean isPermanent;

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
}
