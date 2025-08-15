package io.xpipe.app.core.check;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.core.OsType;

import java.util.concurrent.TimeUnit;

public class AppSystemFontCheck {

    public static void init() {
        if (OsType.getLocal() != OsType.LINUX) {
            return;
        }

        if (hasFonts()) {
            return;
        }

        System.setProperty(
                "prism.fontdir",
                AppInstallation.ofCurrent().getBundledFontsPath().toString());
        System.setProperty("prism.embeddedfonts", "true");
    }

    private static boolean hasFonts() {
        var fc = new ProcessBuilder("fc-match").redirectError(ProcessBuilder.Redirect.DISCARD);
        try {
            var proc = fc.start();
            var out = new String(proc.getInputStream().readAllBytes());
            proc.waitFor(1, TimeUnit.SECONDS);
            return proc.exitValue() == 0 && !out.isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}
