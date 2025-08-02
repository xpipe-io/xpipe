package io.xpipe.app.core.check;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.LocalExec;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AppHomebrewCoreutilsCheck {

    public static Optional<String> checkCoreutils() {
        var r = LocalExec.readStdoutIfPossible("which", "stat");
        if (r.isEmpty()) {
            return Optional.empty();
        }

        var first = r.get().lines().findFirst();
        return first.filter(s -> s.contains("coreutils"))
                .map(s -> FilePath.of(s).getParent().toString());
    }

    public static void check() {
        if (!OsType.getLocal().equals(OsType.MACOS)) {
            return;
        }

        var loc = checkCoreutils();
        if (loc.isPresent()) {
            ErrorEventFactory.fromMessage("You have the homebrew coreutils package installed and added to your PATH at "
                            + loc.get() + "."
                            + " The coreutils commands overwrite and are incompatible to the native macOS commands, which XPipe expects."
                            + " XPipe will fall back to sh while the coreutils package is in the zsh PATH."
                            + " Once you remove coreutils from the PATH, you can switch back to zsh in Settings -> System -> Enable fallback shell")
                    .expected()
                    .handle();
        }
    }
}
