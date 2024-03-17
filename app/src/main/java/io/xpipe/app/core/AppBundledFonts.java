package io.xpipe.app.core;

import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeInstallation;
import javafx.scene.text.Font;

import java.util.concurrent.TimeUnit;

public class AppBundledFonts {

    public static void init() {
        if (OsType.getLocal() != OsType.LINUX) {
            return;
        }

        if (hasFonts() && canLoadFonts()) {
            return;
        }

        System.setProperty(
                "prism.fontdir", XPipeInstallation.getBundledFontsPath().toString());
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

    private static boolean canLoadFonts() {
        try {
            // This can fail if the found system fonts can somehow not be loaded
            Font.getDefault();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
