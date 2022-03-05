package io.xpipe.beacon;

public class BeaconException extends RuntimeException {

    public BeaconException() {
    }

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
