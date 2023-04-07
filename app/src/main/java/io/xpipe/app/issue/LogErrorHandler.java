package io.xpipe.app.issue;

import io.xpipe.app.core.AppLogs;
import io.xpipe.core.util.Deobfuscator;

public class LogErrorHandler implements ErrorHandler {

    @Override
    public void handle(ErrorEvent event) {
        if (AppLogs.get() != null) {
            if (event.getThrowable() != null) {
                AppLogs.get().logException(event.getDescription(), event.getThrowable());
            } else {
                AppLogs.get()
                        .logEvent(TrackEvent.fromMessage("error", event.getDescription())
                                .build());
            }
            return;
        }

        if (event.getDescription() != null) {
            System.err.println(event.getDescription());
        }
        if (event.getThrowable() != null) {
            Deobfuscator.printStackTrace(event.getThrowable());
        }
    }
}
