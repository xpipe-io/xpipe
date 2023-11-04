package io.xpipe.app.ext;

public class ExtensionException extends RuntimeException {

    public ExtensionException() {}

    public ExtensionException(String message) {
        super(message);
    }

    public ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtensionException(Throwable cause) {
        super(cause);
    }

    public ExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static ExtensionException corrupt(String message) {
        return new ExtensionException(message + ". Is the installation corrupt?");
    }
}
