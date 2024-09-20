package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEvent;

public class AppJavaOptionsCheck {

    public static void check() {
        var env = System.getenv("_JAVA_OPTIONS");
        if (env == null) {
            return;
        }

        ErrorEvent.fromMessage(
                        "You have configured the environment variable _JAVA_OPTIONS=%s on your system.".formatted(env)
                                + " This will forcefully apply all custom JVM options to XPipe as well and can cause a variety of different issues."
                                + " Please remove this global environment variable and use local configuration instead for your other JVM programs.")
                .expected()
                .handle();
    }
}
