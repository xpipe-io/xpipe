package io.xpipe.app.beacon;

import io.xpipe.beacon.BeaconClientException;
import lombok.Value;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class AppBeaconCache {

    Set<BeaconShellSession> shellSessions = new HashSet<>();
    Map<UUID, byte[]> savedBlobs = new ConcurrentHashMap<>();

    public BeaconShellSession getShellSession(UUID uuid) throws BeaconClientException {
        var found = shellSessions.stream().filter(beaconShellSession -> beaconShellSession.getEntry().getUuid().equals(uuid)).findFirst();
        if (found.isEmpty()) {
            throw new BeaconClientException("No active shell session known for id " + uuid);
        }
        return found.get();
    }

    public byte[] getBlob(UUID uuid) throws BeaconClientException {
        var found = savedBlobs.get(uuid);
        if (found == null) {
            throw new BeaconClientException("No saved data known for id " + uuid);
        }
        return found;
    }
}
