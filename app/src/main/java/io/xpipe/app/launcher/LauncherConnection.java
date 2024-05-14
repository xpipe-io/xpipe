package io.xpipe.app.launcher;

import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconClientInformation;
import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.BeaconConnectorException;

public class LauncherConnection extends BeaconConnection {

    @Override
    protected void establishConnection() throws Exception {
        try {
            beaconClient = BeaconClient.establishConnection(
                    BeaconClientInformation.DaemonInformation.builder().build());
        } catch (Exception ex) {
            throw new BeaconConnectorException("Unable to connect to running xpipe daemon", ex);
        }
    }
}
