package io.xpipe.beacon;

/**
 * An unchecked exception that will be thrown in any case of an underlying exception.
 */
public class BeaconException extends RuntimeException {

    public BeaconException() {}

    public BeaconException(String message) {
        super(message);
    }

    public BeaconException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeaconException(Throwable cause) {
        super(cause);
    }

    public BeaconException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
