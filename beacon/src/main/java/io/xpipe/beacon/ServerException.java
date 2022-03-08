package io.xpipe.beacon;

import lombok.experimental.StandardException;

/**
 * Indicates that an internal server error occurred.
 */
@StandardException
public class ServerException extends Exception {

    public ServerException() {
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerException(Throwable cause) {
        super(cause);
    }

    public ServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
