package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LinuxLibC;
import io.xpipe.app.util.OsType;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class AppSid {

    @Getter
    private static boolean hasSetsid;

    public static void init() {
        if (OsType.ofLocal() == OsType.WINDOWS) {
            return;
        }

        var checkProcess = new ProcessBuilder("which", "setsid")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        try {
            var p = checkProcess.start();
            if (p.waitFor(1000, TimeUnit.MILLISECONDS)) {
                hasSetsid = p.exitValue() == 0;
            }
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
        }

        if (hasSetsid) {
            TrackEvent.info("Found setsid command");
            return;
        }

        // Don't set this in development mode or debug mode
        if (AppProperties.get().isDevelopmentEnvironment()
                || AppLogs.get().getLogLevel().equals("trace")) {
            return;
        }

        try {
            // If there is no setsid command, we can't fully prevent commands from accessing any potential parent tty
            // We can however set the pid to prevent this happening when launched from the cli command
            // If we launched the daemon executable itself, this has no effect
            var lib = LinuxLibC.getLibrary();
            if (lib.isPresent()) {
                lib.get().setsid();
                TrackEvent.info("Successfully set process sid");
            }
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).omit().handle();
        }
    }
}
