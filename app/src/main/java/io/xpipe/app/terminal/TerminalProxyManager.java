package io.xpipe.app.terminal;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Value;

import java.util.Optional;
import java.util.UUID;

public class TerminalProxyManager {

    private static ActiveSession activeSession;

    public static boolean canUseAsProxy(DataStoreEntryRef<ShellStore> ref) {
        if (!ref.get().getValidity().isUsable()) {
            return false;
        }

        var parent = DataStorage.get().getDefaultDisplayParent(ref.get());
        if (parent.isEmpty()) {
            return false;
        }

        if (!parent.get().equals(DataStorage.get().local())
                && !DataStorage.get()
                        .local()
                        .equals(DataStorage.get()
                                .getDefaultDisplayParent(parent.get())
                                .orElse(null))) {
            return false;
        }

        var id = ref.get().getProvider().getId();
        return id.equals("wsl");
    }

    public static Optional<ShellControl> getProxy() {
        var uuid = AppPrefs.get().terminalProxy().getValue();
        var hasCustomTerminalShell =
                uuid != null && !DataStorage.get().local().getUuid().equals(uuid);
        if (!hasCustomTerminalShell) {
            return Optional.empty();
        }

        var foundEntry = DataStorage.get().getStoreEntryIfPresent(uuid);
        if (foundEntry.isEmpty()) {
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
                ErrorEventFactory.fromThrowable(ex).handle();
                activeSession = new ActiveSession(uuid, null);
                return Optional.empty();
            }
        }

        try {
            var control = createControl(foundEntry.get().ref());
            if (control.isPresent()) {
                control.get().start();
                activeSession = new ActiveSession(uuid, control.get());
                return control;
            }
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
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
        return Optional.of(control);
    }

    @Value
    private static class ActiveSession {
        UUID uuid;
        ShellControl control;
    }
}
