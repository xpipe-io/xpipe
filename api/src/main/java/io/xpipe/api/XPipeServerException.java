package io.xpipe.api;

public class XPipeServerException extends RuntimeException {

    public XPipeServerException() {
    }

    public XPipeServerException(String message) {
        super(message);
    }

    public XPipeServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public XPipeServerException(Throwable cause) {
        super(cause);
    }
}
