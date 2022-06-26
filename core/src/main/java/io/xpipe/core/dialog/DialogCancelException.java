package io.xpipe.core.dialog;

public class DialogCancelException extends Exception {

    public DialogCancelException() {
    }

    public DialogCancelException(String message) {
        super(message);
    }

    public DialogCancelException(String message, Throwable cause) {
        super(message, cause);
    }

    public DialogCancelException(Throwable cause) {
        super(cause);
    }

    public DialogCancelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
