package io.xpipe.app.beacon;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.ShellStore;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AppBeaconCache {

    private final Set<BeaconShellSession> shellSessions = new HashSet<>();

    public synchronized Set<BeaconShellSession> getShellSessions() {
        return Set.copyOf(shellSessions);
    }

    public synchronized Optional<BeaconShellSession> findShellSession(UUID uuid) {
        return shellSessions.stream()
                .filter(beaconShellSession ->
                        beaconShellSession.getEntry().getUuid().equals(uuid))
                .findFirst();
    }

    public synchronized void removeAndCloseShellSession(UUID uuid) throws Exception {
        var found = findShellSession(uuid);
        if (found.isEmpty()) {
            throw new BeaconClientException("No active shell session known for id " + uuid);
        }

        try {
            found.get().getControl().close();
        } finally {
            shellSessions.removeIf(beaconShellSession ->
                    beaconShellSession.getEntry().getUuid().equals(uuid));
        }
    }

    public synchronized BeaconShellSession getRunningShellSession(UUID uuid) throws Exception {
        var found = findShellSession(uuid);
        if (found.isEmpty()) {
            throw new BeaconClientException("No active shell session known for id " + uuid);
        }

        var sc = found.get().getControl();
        if (!sc.isRunning(true) || sc.isAnyStreamClosed()) {
            throw new BeaconServerException("Shell session for " + uuid + " exited");
        }

        return found.get();
    }

    public synchronized BeaconShellSession getOrStart(DataStoreEntryRef<ShellStore> ref) throws Exception {
        var existing = shellSessions.stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(ref.get()))
                .findFirst();
        if (existing.isPresent()) {
            var sc = existing.get().getControl();
            if (!sc.isRunning(true) || sc.isAnyStreamClosed()) {
                sc.restart();
            }
            return existing.get();
        }

        var session = new BeaconShellSession(ref.get(), ref.getStore().standaloneControl());
        session.getControl().setNonInteractive();
        session.getControl().start();
        shellSessions.add(session);
        return session;
    }
}
