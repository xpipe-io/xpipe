package io.xpipe.beacon;

/**
 * Indicates that a client request was invalid.
 */
public class BeaconClientException extends Exception {

    public BeaconClientException(String message) {
        super(message);
    }

    public BeaconClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeaconClientException(Throwable cause) {
        super(cause);
    }

    public BeaconClientException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
