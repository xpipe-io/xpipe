package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;

import lombok.Getter;

@Getter
public enum CloseBehaviour implements PrefsChoiceValue {
    QUIT("app.quit") {
        @Override
        public void run() {
            OperationMode.shutdown(false, false);
        }
    },

    MINIMIZE_TO_TRAY("app.minimizeToTray") {
        @Override
        public void run() {
            OperationMode.switchToAsync(OperationMode.TRAY);
        }

        @Override
        public boolean isSelectable() {
            return OperationMode.TRAY.isSupported();
        }
    },

    CONTINUE_IN_BACKGROUND("app.continueInBackground") {
        @Override
        public void run() {
            OperationMode.switchToAsync(OperationMode.BACKGROUND);
        }

        @Override
        public boolean isSelectable() {
            return !OperationMode.TRAY.isSupported();
        }
    };

    private final String id;

    CloseBehaviour(String id) {
        this.id = id;
    }

    public abstract void run();
}
