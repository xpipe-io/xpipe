package io.xpipe.beacon;

/**
 * Indicates that a connection error occurred.
 */
public class BeaconConnectorException extends Exception {

    public BeaconConnectorException() {}

    public BeaconConnectorException(String message) {
        super(message);
    }

    public BeaconConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeaconConnectorException(Throwable cause) {
        super(cause);
    }

    public BeaconConnectorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
