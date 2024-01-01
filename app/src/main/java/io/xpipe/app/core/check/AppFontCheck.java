package io.xpipe.app.core.check;

import javafx.scene.text.Font;

public class AppFontCheck {

    public static void check() {
        try {
            Font.getDefault();
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to load any fonts. Check whether your system is properly configured with fontconfig and you have any fonts installed", t);
        }
    }

}
