package io.xpipe.app.beacon;

import io.xpipe.beacon.BeaconClientException;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Value
public class AppBeaconCache {

    Set<BeaconShellSession> shellSessions = new HashSet<>();

    public BeaconShellSession getShellSession(UUID uuid) throws BeaconClientException {
        var found = shellSessions.stream().filter(beaconShellSession -> beaconShellSession.getEntry().getUuid().equals(uuid)).findFirst();
        if (found.isEmpty()) {
            throw new BeaconClientException("No active shell session known for id " + uuid);
        }
        return found.get();
    }
}
