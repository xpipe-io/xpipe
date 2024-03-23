package io.xpipe.app.core.check;

import javafx.scene.text.Font;

public class AppFontLoadingCheck {

    public static void check() {
        try {
            // This can fail if the found system fonts can somehow not be loaded
            Font.getDefault();
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to load fonts", e);
        }
    }
}
