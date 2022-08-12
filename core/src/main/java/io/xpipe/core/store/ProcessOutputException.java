package io.xpipe.core.store;

public class ProcessOutputException extends Exception{
    public ProcessOutputException() {
        super();
    }

    public ProcessOutputException(String message) {
        super(message);
    }

    public ProcessOutputException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessOutputException(Throwable cause) {
        super(cause);
    }

    protected ProcessOutputException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
