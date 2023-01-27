package io.xpipe.cli.util;

import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.BeaconException;
import io.xpipe.beacon.BeaconServer;
import io.xpipe.core.util.XPipeDaemonMode;

import java.util.Optional;

public class XPipeCliConnection extends BeaconConnection {

    private boolean stopDaemonOnExit;

    public static XPipeCliConnection open() {
        var con = new XPipeCliConnection();
        con.constructSocket();
        return con;
    }

    private static Process startInstallation() throws Exception {
        var installation = CliHelper.findInstallation();
        var res = BeaconServer.start(installation, XPipeDaemonMode.BACKGROUND);
        if (res != null) {
            return res;
        }

        throw new UnsupportedOperationException("Unable to start xpiped");
    }

    public static Optional<BeaconClient> waitForStartup(Process process) {
        var inDev = !CliHelper.isProduction();
        var max = inDev ? 160 : 40;
        try (var spinner = BusySpinner.start("Starting X-Pipe daemon ...", true)) {
            for (int i = 0; i < max; i++) {
                if (process != null && !process.isAlive() && process.exitValue() != 0) {
                    break;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }

                var s = BeaconClient.tryConnect(BeaconClient.CliClientInformation.builder()
                        .consoleWidth(80)
                        .build());
                if (s.isPresent()) {
                    spinner.close();
                    System.out.println("X-Pipe daemon started in " + (i / 2) + "s");
                    return s;
                }
            }
        }
        return Optional.empty();
    }

    public boolean isStopDaemonOnExit() {
        return stopDaemonOnExit;
    }

    public void stopDaemon() throws Exception {
        BeaconServer.tryStop(beaconClient);
    }

    @Override
    protected void constructSocket() {
        if (!BeaconServer.isRunning()) {
            var custom = false;
            Process process = null;
            try {
                if ((process = BeaconServer.tryStartCustom()) != null) {
                    custom = true;
                } else {
                    process = startInstallation();
                }
            } catch (Exception ex) {
                throw new BeaconException("Unable to start xpipe daemon", ex);
            }

            var r = waitForStartup(process);
            if (r.isEmpty()) {
                throw new BeaconException("Wait for xpipe daemon startup timed out");
            } else {
                beaconClient = r.get();
                this.stopDaemonOnExit = custom;
                return;
            }
        }

        try {
            beaconClient = BeaconClient.connect(
                    BeaconClient.CliClientInformation.builder().consoleWidth(80).build());
        } catch (Exception ex) {
            throw new BeaconException("Unable to connect to running xpipe daemon", ex);
        }
    }
}
