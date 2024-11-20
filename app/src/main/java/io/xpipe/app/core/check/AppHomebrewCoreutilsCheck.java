package io.xpipe.app.core.check;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.Getter;

import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AppHomebrewCoreutilsCheck {

    public static boolean getResult() {
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

        if (!getResult()) {
            ErrorEvent.fromMessage("You have the homebrew coreutils package installed and added to your PATH." +
                            " The coreutils commands overwrite and are incompatible to the native macOS commands, which XPipe expects." +
                            " Please remove the coreutils commands from your PATH prior to launching XPipe.")
                    .noDefaultActions()
                    .term()
                    .handle();
        }
    }
}
