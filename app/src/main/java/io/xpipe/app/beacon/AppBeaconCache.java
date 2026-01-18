package io.xpipe.app.beacon;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.beacon.BeaconClientException;

import lombok.Value;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class AppBeaconCache {

    Map<UUID, BeaconShellSession> shellSessionsById = new ConcurrentHashMap<>();

    public Collection<BeaconShellSession> getShellSessions() {
        return shellSessionsById.values();
    }

    public BeaconShellSession getShellSession(UUID uuid) throws BeaconClientException {
        var session = shellSessionsById.get(uuid);
        if (session == null) {
            throw new BeaconClientException("No active shell session known for id " + uuid);
        }
        return session;
    }

    public BeaconShellSession getOrStart(DataStoreEntryRef<ShellStore> ref) throws Exception {
        var entry = ref.get();
        try {
            var session = shellSessionsById.computeIfAbsent(entry.getUuid(), key -> {
                try {
                    var control = ref.getStore().standaloneControl();
                    control.setNonInteractive();
                    control = control.start();

                    var d = control.getShellDialect().getDumbMode();
                    if (!d.supportsAnyPossibleInteraction()) {
                        control.close();
                        d.throwIfUnsupported();
                    }

                    return new BeaconShellSession(entry, control);
                } catch (Exception ex) {
                    throw new ShellSessionInitException(ex);
                }
            });
            session.getControl().setNonInteractive();
            return session;
        } catch (ShellSessionInitException ex) {
            throw ex.getCause();
        }
    }

    public void removeShellSession(UUID uuid) {
        shellSessionsById.remove(uuid);
    }

    private static final class ShellSessionInitException extends RuntimeException {

        private final Exception cause;

        private ShellSessionInitException(Exception cause) {
            super(cause);
            this.cause = cause;
        }

        @Override
        public synchronized Exception getCause() {
            return cause;
        }
    }
}
