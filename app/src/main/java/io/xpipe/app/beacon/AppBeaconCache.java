package io.xpipe.app.beacon;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.beacon.BeaconClientException;

import lombok.Value;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Value
public class AppBeaconCache {

    Set<BeaconShellSession> shellSessions = new HashSet<>();

    public BeaconShellSession getShellSession(UUID uuid) throws BeaconClientException {
        var found = shellSessions.stream()
                .filter(beaconShellSession ->
                        beaconShellSession.getEntry().getUuid().equals(uuid))
                .findFirst();
        if (found.isEmpty()) {
            throw new BeaconClientException("No active shell session known for id " + uuid);
        }
        return found.get();
    }

    public BeaconShellSession getOrStart(DataStoreEntryRef<ShellStore> ref) throws Exception {
        var existing = AppBeaconServer.get().getCache().getShellSessions().stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(ref.get()))
                .findFirst();
        var control = (existing.isPresent()
                ? existing.get().getControl()
                : ref.getStore().standaloneControl().start());
        control.setNonInteractive();
        control.start();

        var d = control.getShellDialect().getDumbMode();
        if (!d.supportsAnyPossibleInteraction()) {
            control.close();
            d.throwIfUnsupported();
        }

        if (existing.isEmpty()) {
            AppBeaconServer.get().getCache().getShellSessions().add(new BeaconShellSession(ref.get(), control));
        }

        return new BeaconShellSession(ref.get(), control);
    }
}
