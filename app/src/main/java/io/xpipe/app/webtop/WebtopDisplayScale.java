package io.xpipe.app.webtop;

import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.PropertiesFormatsParser;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.GlobalTimer;

import java.time.Duration;
import java.util.OptionalInt;

public class WebtopDisplayScale {

    private static Integer initialScale;

    public static void init() {
        if (AppDistributionType.get() != AppDistributionType.WEBTOP) {
            return;
        }

        initialScale = AppPrefs.get().uiScale().getValue();

        GlobalTimer.scheduleUntil(Duration.ofSeconds(5), true, () -> {
            try {
                var dpi = getDpi();
                if (dpi.isEmpty()) {
                    return false;
                }

                var mult = (int) Math.round((dpi.getAsInt() / 96.0) * 100.0);
                var clamped = AppDisplayScale.clampValue(mult);

                if (initialScale == null) {
                    initialScale = clamped;
                } else if (!initialScale.equals(clamped)) {
                    AppPrefs.get().uiScale.setValue(clamped);
                    AppRestart.restart();
                    return true;
                }

                return false;
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return true;
            }
        });
    }

    private static OptionalInt getDpi() throws Exception {
        var out = LocalShell.getShell().command(CommandBuilder.of().add("xrdb", "-query"))
                .sensitive()
                .readStdoutIfPossible();
        if (out.isEmpty()) {
            return OptionalInt.empty();
        }

        var found = PropertiesFormatsParser.parse(out.get(), ":").get("Xft.dpi");
        return found != null ? OptionalInt.of(Integer.parseInt(found)) : OptionalInt.empty();
    }
}
