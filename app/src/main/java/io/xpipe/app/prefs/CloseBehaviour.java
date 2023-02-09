package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import lombok.Getter;

@Getter
public enum CloseBehaviour implements PrefsChoiceValue {
    QUIT("app.quit", () -> {
        OperationMode.shutdown(false, false);
    }),

    CONTINUE_IN_BACKGROUND("app.continueInBackground", () -> {
        OperationMode.switchToAsync(OperationMode.BACKGROUND);
    }),

    MINIMIZE_TO_TRAY("app.minimizeToTray", () -> {
        OperationMode.switchToAsync(OperationMode.TRAY);
    });

    private String id;
    private Runnable exit;

    CloseBehaviour(String id, Runnable exit) {
        this.id = id;
        this.exit = exit;
    }

    public boolean isSelectable() {
        return true;
    }
}
