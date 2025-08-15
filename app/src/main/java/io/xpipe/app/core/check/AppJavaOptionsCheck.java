package io.xpipe.app.core.check;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.issue.ErrorEventFactory;

public class AppJavaOptionsCheck {

    public static void check() {
        if (AppCache.getBoolean("javaOptionsWarningShown", false)) {
            return;
        }

        var env = System.getenv("_JAVA_OPTIONS");
        if (env == null || env.isBlank()) {
            return;
        }

        ErrorEventFactory.fromMessage(
                        "You have configured the global environment variable _JAVA_OPTIONS=%s on your system."
                                        .formatted(env)
                                + " This will forcefully apply all custom JVM options to " + AppNames.ofCurrent().getName() + " and can cause a variety of different issues."
                                + " Please remove this global environment variable and use local configuration instead for your other JVM programs.")
                .expected()
                .handle();
        AppCache.update("javaOptionsWarningShown", true);
    }
}
