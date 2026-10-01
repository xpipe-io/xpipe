package io.xpipe.app.ext;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;

public class ExtensionException extends RuntimeException {

    public ExtensionException() {}

    public ExtensionException(String message) {
        super(message);
    }

    private ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ExtensionException corrupt(String message, Throwable cause) {
        if (AppProperties.get().isDevelopmentEnvironment()) {
            var full = message + ".\n\n" + "Please check whether you followed the dev build setups instructions and installed" +
                    " a local XPipe production build with matching version";
            return new ExtensionException(full, cause);
        }

        var loc = AppInstallation.ofCurrent().getBaseInstallationPath();
        var full = message + ".\n\n" + "Please check whether the "
                + AppNames.ofCurrent().getName() + " installation data at " + loc + " is corrupted.";
        return new ExtensionException(full, cause);
    }

    public static ExtensionException corrupt(String message) {
        return corrupt(message, null);
    }
}
