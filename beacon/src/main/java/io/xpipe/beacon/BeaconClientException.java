package io.xpipe.beacon;

/**
 * Indicates that a client request was invalid.
 */
public class BeaconClientException extends Exception {

    public BeaconClientException(String message) {
        super(message);
    }
}
