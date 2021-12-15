package io.xpipe.api;

public class XPipeClientException extends RuntimeException {

    public XPipeClientException() {
    }

    public XPipeClientException(String message) {
        super(message);
    }

    public XPipeClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public XPipeClientException(Throwable cause) {
        super(cause);
    }
}
