package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AppHomebrewCoreutilsCheck {

    public static Optional<String> checkCoreutils() {
        var fc = new ProcessBuilder("which", "stat").redirectErrorStream(true);
        try {
            var proc = fc.start();
            var out = new String(proc.getInputStream().readAllBytes());
            proc.waitFor(1, TimeUnit.SECONDS);
            var first = out.lines().findFirst();
            return first.filter(s -> s.contains("coreutils")).map(s -> FileNames.getParent(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void check() {
        if (!OsType.getLocal().equals(OsType.MACOS)) {
            return;
        }

        var loc = checkCoreutils();
        if (loc.isPresent()) {
            ErrorEvent.fromMessage("You have the homebrew coreutils package installed and added to your PATH at " + loc.get() + "." +
                            " The coreutils commands overwrite and are incompatible to the native macOS commands, which XPipe expects." +
                            " Please remove the coreutils commands from your PATH prior to launching XPipe.")
                    .term()
                    .handle();
        }
    }
}
