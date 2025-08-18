package io.xpipe.app.ext;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppNames;

public class ExtensionException extends RuntimeException {

    public ExtensionException() {}

    private ExtensionException(String message) {
        super(message);
    }

    private ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtensionException(Throwable cause) {
        super(cause);
    }

    public ExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static ExtensionException corrupt(String message, Throwable cause) {
        try {
            var loc = AppInstallation.ofCurrent().getBaseInstallationPath();
            var full = message + ".\n\n" + "Please check whether the "
                    + AppNames.ofCurrent().getName() + " installation data at " + loc + " is corrupted.";
            return new ExtensionException(full, cause);
        } catch (Throwable t) {
            var full = message + ".\n\n" + "Please check whether the "
                    + AppNames.ofCurrent().getName() + " installation data is corrupted.";
            return new ExtensionException(full, cause);
        }
    }

    public static ExtensionException corrupt(String message) {
        return corrupt(message, null);
    }
}
