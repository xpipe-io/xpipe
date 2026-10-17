package io.xpipe.app.core.check;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.util.LocalExec;
import io.xpipe.app.util.OsType;

public class AppSystemFontCheck {

    public static void init() {
        if (OsType.ofLocal() != OsType.LINUX) {
            return;
        }

        if (hasFonts()) {
            return;
        }

        System.setProperty(
                "prism.fontdir",
                AppInstallation.ofCurrent()
                        .getBaseInstallationPath()
                        .resolve("fonts")
                        .toString());
        System.setProperty("prism.embeddedfonts", "true");
    }

    private static boolean hasFonts() {
        var out = LocalExec.readStdoutIfPossible("fc-match");
        return out.isPresent() && !out.get().isBlank();
    }
}
