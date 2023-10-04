package io.xpipe.app.util;

public class LicenseException extends RuntimeException {

    public LicenseException() {
    }

    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LicenseException(Throwable cause) {
        super(cause);
    }

    public LicenseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LicenseException(String featureName, LicenseType min) {
        this(featureName + " are only supported with a " + min.name().toLowerCase() + " license");
    }
}
