package io.xpipe.api;

public class XPipeException extends RuntimeException {

    public XPipeException() {
    }

    public XPipeException(String message) {
        super(message);
    }

    public XPipeException(String message, Throwable cause) {
        super(message, cause);
    }

    public XPipeException(Throwable cause) {
        super(cause);
    }

    public XPipeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
