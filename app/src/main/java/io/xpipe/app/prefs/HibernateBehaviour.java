package io.xpipe.app.prefs;

import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.secret.DataStorageAccessHandler;

import lombok.Getter;

@Getter
public enum HibernateBehaviour implements PrefsChoiceValue {
    LOCK_VAULT("lockVault") {
        @Override
        public void runOnWake() {
            AppRestart.restart();
        }

        @Override
        public void runOnSleep() {
            AppOperationMode.switchToAsync(AppOperationMode.BACKGROUND);
        }

        @Override
        public boolean isAvailable() {
            var handler = DataStorageAccessHandler.getInstance();
            return handler != null && handler.isAccessRestricted();
        }
    },

    RESTART("restart") {
        @Override
        public void runOnWake() {
            AppRestart.restart();
        }

        @Override
        public void runOnSleep() {
            AppOperationMode.switchToAsync(AppOperationMode.BACKGROUND);
        }
    };

    private final String id;

    HibernateBehaviour(String id) {
        this.id = id;
    }

    public abstract void runOnSleep();

    public abstract void runOnWake();
}
