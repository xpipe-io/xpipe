package io.xpipe.app.core.check;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LocalExec;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.util.Optional;

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
        if (OsType.ofLocal() != OsType.MACOS) {
            return;
        }

        if (LocalShell.getDialect() != ShellDialects.ZSH) {
            return;
        }

        var loc = checkCoreutils();
        if (loc.isPresent()) {
            ErrorEventFactory.fromMessage("You have the homebrew coreutils package installed and added to your PATH at "
                            + loc.get() + "."
                            + " The coreutils commands overwrite and are incompatible to the native macOS commands, which "
                            + AppNames.ofCurrent().getName() + " expects."
                            + " Please remove the coreutils commands from your PATH prior to launching XPipe.")
                    .expected()
                    .handle();
        }
    }
}
