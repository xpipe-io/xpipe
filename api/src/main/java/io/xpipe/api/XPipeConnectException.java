package io.xpipe.api;

public class XPipeConnectException extends RuntimeException {

    public XPipeConnectException() {
    }

    public XPipeConnectException(String message) {
        super(message);
    }

    public XPipeConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public XPipeConnectException(Throwable cause) {
        super(cause);
    }
}
