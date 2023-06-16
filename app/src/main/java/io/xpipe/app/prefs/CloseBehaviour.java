package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;
import lombok.Getter;

@Getter
public enum CloseBehaviour implements PrefsChoiceValue {
    QUIT("app.quit", () -> {
        OperationMode.shutdown(false, false);
    }),

    MINIMIZE_TO_TRAY("app.minimizeToTray", () -> {
        OperationMode.switchToAsync(OperationMode.TRAY);
    });

    private final String id;
    private final Runnable exit;

    CloseBehaviour(String id, Runnable exit) {
        this.id = id;
        this.exit = exit;
    }

    public boolean isSelectable() {
        return true;
    }
}
