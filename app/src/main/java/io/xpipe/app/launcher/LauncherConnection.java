package io.xpipe.app.launcher;

import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.BeaconException;

public class LauncherConnection extends BeaconConnection {

    @Override
    protected void constructSocket() {
        try {
            beaconClient = BeaconClient.connect(
                    BeaconClient.DaemonInformation.builder().build());
        } catch (Exception ex) {
            throw new BeaconException("Unable to connect to running xpipe daemon", ex);
        }
    }
}
