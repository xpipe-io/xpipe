package io.xpipe.app.core.check;

import io.xpipe.core.util.XPipeInstallation;
import javafx.scene.text.Font;

public class AppFontLoadingCheck {

    public static void init() {
        if (canLoadFonts()) {
            return;
        }

        if (System.getProperty("prism.fontdir") != null) {
            throw new IllegalStateException("Unable to load bundled fonts");
        }

        System.setProperty("prism.fontdir", XPipeInstallation.getBundledFontsPath().toString());
        System.setProperty("prism.embeddedfonts", "true");
        init();
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
