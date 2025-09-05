package io.xpipe.app.prefs;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;

import lombok.Getter;

@Getter
public enum CloseBehaviour implements PrefsChoiceValue {
    QUIT("app.quit") {
        @Override
        public void run() {
            AppOperationMode.shutdown(false);
        }
    },

    MINIMIZE_TO_TRAY("app.minimizeToTray") {
        @Override
        public void run() {
            AppOperationMode.switchToAsync(AppOperationMode.TRAY);
        }

        @Override
        public boolean isSelectable() {
            return AppOperationMode.TRAY.isSupported();
        }
    },

    CONTINUE_IN_BACKGROUND("app.continueInBackground") {
        @Override
        public void run() {
            AppOperationMode.switchToAsync(AppOperationMode.BACKGROUND);
        }

        @Override
        public boolean isSelectable() {
            return !AppOperationMode.TRAY.isSupported();
        }
    };

    private final String id;

    CloseBehaviour(String id) {
        this.id = id;
    }

    public abstract void run();
}
