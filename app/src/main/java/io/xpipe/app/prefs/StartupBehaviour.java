package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.core.util.XPipeDaemonMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StartupBehaviour implements PrefsChoiceValue {
    GUI("app.startGui", XPipeDaemonMode.GUI),
    TRAY("app.startInTray", XPipeDaemonMode.TRAY);

    private final String id;
    private final XPipeDaemonMode mode;

    public boolean isSelectable() {
        return true;
    }
}
