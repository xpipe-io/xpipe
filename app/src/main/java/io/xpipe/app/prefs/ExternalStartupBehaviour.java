package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExternalStartupBehaviour implements PrefsChoiceValue {
    GUI("app.startGui", OperationMode.GUI),
    TRAY("app.startInTray", OperationMode.TRAY),
    BACKGROUND("app.startInBackground", OperationMode.BACKGROUND);

    private final String id;
    private final OperationMode mode;

    public boolean isSelectable() {
        return true;
    }
}
