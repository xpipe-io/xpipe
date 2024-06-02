package io.xpipe.beacon;

/**
 * Indicates that an internal server error occurred.
 */
public class BeaconServerException extends Exception {

    public BeaconServerException(String message) {
        super(message);
    }

    public BeaconServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeaconServerException(Throwable cause) {
        super(cause);
    }

    public BeaconServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
