package io.xpipe.app.prefs;

import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.util.ThreadHelper;
import lombok.Getter;

@Getter
public enum HibernateBehaviour implements PrefsChoiceValue {
    LOCK_VAULT("lockVault") {
        @Override
        public void run() {
            var handler = DataStorageUserHandler.getInstance();
            if (handler != null && handler.getActiveUser() != null) {
                // If we run this at the same time as the system is waking, there might be exceptions
                // because the platform does not like being shut down while still kinda sleeping
                // This assures that it will be run later, on system wake
                ThreadHelper.runAsync(() -> {
                    ThreadHelper.sleep(1000);
                    AppOperationMode.close();
                });
            }
        }
    },

    RESTART("restart") {
        @Override
        public void run() {
            AppRestart.restart();
        }
    };

    private final String id;

    HibernateBehaviour(String id) {
        this.id = id;
    }

    public abstract void run();
}
