package io.xpipe.app.prefs;

import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.value.ObservableValue;

import lombok.Getter;

@Getter
public enum HibernateBehaviour implements PrefsChoiceValue {
    LOCK_VAULT("lockVault") {
        @Override
        public void runOnWake() {}

        @Override
        public void runOnSleep() {
            var handler = DataStorageUserHandler.getInstance();
            if (handler != null && handler.getActiveUser() != null) {
                ThreadHelper.runAsync(() -> {
                    AppOperationMode.close();
                });
            }
        }
    },

    RESTART("restart") {
        @Override
        public ObservableValue<String> toTranslatedString() {
            return super.toTranslatedString();
        }

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
