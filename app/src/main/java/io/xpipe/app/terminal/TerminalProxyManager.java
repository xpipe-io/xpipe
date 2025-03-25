package io.xpipe.app.terminal;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.TerminalInitFunction;
import io.xpipe.core.process.TerminalInitScriptConfig;
import io.xpipe.core.process.WorkingDirectoryFunction;
import io.xpipe.core.store.DataStore;
import lombok.Value;

import java.util.Optional;
import java.util.UUID;

public class TerminalProxyManager {

    @Value
    private static class ActiveSession {
        UUID uuid;
        ShellControl control;
    }

    private static ActiveSession activeSession;

    public static Optional<ShellControl> getProxy() {
        var uuid = AppPrefs.get().terminalProxy().getValue();
        var hasCustomTerminalShell = uuid != null &&
                !DataStorage.get().local().getUuid().equals(uuid);
         if (!hasCustomTerminalShell) {
            return Optional.empty();
        }

        var matchingSession = activeSession != null && activeSession.uuid.equals(uuid) ? activeSession : null;
        if (matchingSession != null) {
            // Probably incompatible
            if (matchingSession.control == null) {
                return Optional.empty();
            }

            try {
                matchingSession.getControl().start();
                return Optional.of(matchingSession.getControl());
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                activeSession = new ActiveSession(uuid, null);
                return Optional.empty();
            }
        }

        DataStoreEntryRef<DataStore> ref = DataStorage.get().getStoreEntry(uuid).ref();
        try {
            var control = createControl(ref);
            if (control.isPresent()) {
                control.get().start();
                activeSession = new ActiveSession(uuid, control.get());
                return control;
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
        activeSession = new ActiveSession(uuid, null);
        return Optional.empty();
    }

    private static Optional<ShellControl> createControl(DataStoreEntryRef<DataStore> ref) throws Exception {
        if (ref == null || !ref.get().getValidity().isUsable() || !(ref.getStore() instanceof ShellStore ss)) {
            return Optional.empty();
        }

        var store = ss;
        var control = store.standaloneControl();
        if (!control.getLocalSystemAccess().supportsExecutables() || !control.getLocalSystemAccess().supportsFileSystemAccess()) {
            return Optional.empty();
        }
        return Optional.of(control);
    }
}
