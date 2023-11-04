package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.util.XPipeDaemonMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StartupBehaviour implements PrefsChoiceValue {
    GUI("app.startGui", XPipeDaemonMode.GUI) {
    },
    TRAY("app.startInTray", XPipeDaemonMode.TRAY) {
        public boolean isSelectable() {
            return OperationMode.TRAY.isSupported();
        }
    },
    BACKGROUND("app.startInBackground", XPipeDaemonMode.BACKGROUND) {
        public boolean isSelectable() {
            return !OperationMode.TRAY.isSupported();
        }
    };

    private final String id;
    private final XPipeDaemonMode mode;
}
