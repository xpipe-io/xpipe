package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;

import java.util.concurrent.TimeUnit;

public class AppHomebrewCoreutilsCheck {

    public static boolean hasCoreutils() {
        var fc = new ProcessBuilder("which", "stat").redirectErrorStream(true);
        try {
            var proc = fc.start();
            var out = new String(proc.getInputStream().readAllBytes());
            proc.waitFor(1, TimeUnit.SECONDS);
            var first = out.lines().findFirst();
            return first.map(s -> s.contains("coreutils")).orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void check() {
        if (!OsType.getLocal().equals(OsType.MACOS)) {
            return;
        }

        if (hasCoreutils()) {
            ErrorEvent.fromMessage("You have the homebrew coreutils package installed and added to your PATH." +
                            " The coreutils commands overwrite and are incompatible to the native macOS commands, which XPipe expects." +
                            " Please remove the coreutils commands from your PATH prior to launching XPipe.")
                    .term()
                    .handle();
        }
    }
}
