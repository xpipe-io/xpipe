package io.xpipe.app.core.check;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.PlatformState;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;

public class AppGpuCheck {

    public static void check() {
        if (PlatformState.getCurrent() != PlatformState.RUNNING) {
            return;
        }

        if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
            return;
        }

        AppPrefs.get().performanceMode.setValue(true);
    }
}
