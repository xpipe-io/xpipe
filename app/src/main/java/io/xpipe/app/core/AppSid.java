package io.xpipe.app.core;

import com.sun.jna.Function;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.OsType;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class AppSid {

    @Getter
    private static boolean hasSetsid;

    public static void check() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return;
        }

        var checkProcess = new ProcessBuilder("which", "setsid").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        try {
            var p = checkProcess.start();
            if (p.waitFor(1000, TimeUnit.MILLISECONDS)) {
                hasSetsid = p.exitValue() == 0;
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().expected().handle();
        }

        if (hasSetsid) {
            TrackEvent.info("Found setsid command");
            return;
        }

        // Don't set this in development mode or debug mode
        if (AppProperties.get().isDevelopmentEnvironment() || AppLogs.get().getLogLevel().equals("trace")) {
            return;
        }

        try {
            // If there is no setsid command, we can't fully prevent commands from accessing any potential parent tty
            // We can however set the pid to prevent this happening when launched from the cli command
            // If we launched the daemon executable itself, this has no effect
            var func = Function.getFunction("c", "setsid");
            func.invoke(new Object[0]);
            TrackEvent.info("Successfully set process sid");
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omit().handle();
        }
    }
}
